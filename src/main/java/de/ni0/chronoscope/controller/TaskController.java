package de.ni0.chronoscope.controller;

import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import de.ni0.chronoscope.config.RequestContext;
import de.ni0.chronoscope.controller.dto.request.DynamicTaskCreateRequest;
import de.ni0.chronoscope.controller.dto.request.DynamicTaskUpdateRequest;
import de.ni0.chronoscope.controller.dto.request.StaticTaskCreateRequest;
import de.ni0.chronoscope.controller.dto.request.StaticTaskUpdateRequest;
import de.ni0.chronoscope.controller.dto.request.TaskCreateRequest;
import de.ni0.chronoscope.controller.dto.request.TaskUpdateRequest;
import de.ni0.chronoscope.controller.dto.response.TaskResponse;
import de.ni0.chronoscope.exception.AccountAccessDeniedException;
import de.ni0.chronoscope.exception.AccountNotFoundException;
import de.ni0.chronoscope.exception.InvalidRequestException;
import de.ni0.chronoscope.mapper.TaskMapper;
import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.DynamicTask;
import de.ni0.chronoscope.model.StaticTask;
import de.ni0.chronoscope.model.Task;
import de.ni0.chronoscope.repository.AccountRepository;
import de.ni0.chronoscope.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Tasks", description = "Create, read, update and delete tasks and their dependencies")
@RestController
@RequestMapping("/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final TaskMapper taskMapper;
    private final AccountRepository accountRepository;
    private final RequestContext requestContext;

    @Operation(summary = "List tasks", description = "Return all tasks belonging to the current identity, including tasks from linked accounts. Each task is either a StaticTask or a DynamicTask, discriminated by the \"type\" field.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })

    @GetMapping
    public List<TaskResponse> getTasks() {
        return taskService.getTasksForIdentity(requestContext.getIdentityId()).stream()
            .map(this::mapTask)
            .toList();
    }

    private TaskResponse mapTask(Task task) {
        return switch (task) {
            case StaticTask staticTask -> taskMapper.toResponse(staticTask);
            case DynamicTask dynamicTask -> taskMapper.toResponse(dynamicTask);
            default -> {
                String taskType = task.getClass().getName();
                String taskId = String.valueOf(task.getId());
                throw new IllegalStateException("Unexpected task subtype in TaskController.mapTask: type=" + taskType + ", taskId=" + taskId);
            }
        };
    }

    @Operation(summary = "Create task", description = "Create a new task. Set \"type\" to \"static\" for a StaticTask or \"dynamic\" for a DynamicTask with scheduling metadata.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Task created"),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody TaskCreateRequest request) {
        TaskResponse response = switch (request) {
            case StaticTaskCreateRequest staticRequest -> {
                Account account = validateAccountOwnership(staticRequest.accountId());
                StaticTask newTask = taskMapper.fromCreateRequest(staticRequest);
                newTask.setAccount(account);
                yield taskMapper.toResponse(taskService.createStaticTask(newTask));
            }
            case DynamicTaskCreateRequest dynamicRequest -> {
                Account account = validateAccountOwnership(dynamicRequest.accountId());
                DynamicTask newTask = taskMapper.fromCreateRequest(dynamicRequest);
                newTask.setAccount(account);
                yield taskMapper.toResponse(taskService.createDynamicTask(newTask));
            }
        };
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private Account validateAccountOwnership(Long requestedAccountId) {
        return accountRepository.findById(requestedAccountId)
                .map(account -> {
                    if (!Objects.equals(account.getIdentity().getId(), requestContext.getIdentityId())) {
                        throw new AccountAccessDeniedException("accountId is not linked to authenticated identity");
                    }
                    return account;
                })
                .orElseThrow(AccountNotFoundException::new);
    }

    @Operation(summary = "Get task", description = "Retrieve a single task by ID, including its labels, scopes (dynamic tasks) and dependencies.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Task retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Task not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public TaskResponse getTask(
            @Parameter(description = "Task ID") @PathVariable Long id) {
        return mapTask(taskService.getTaskForIdentity(requestContext.getIdentityId(), id));
    }

    @Operation(summary = "Update task", description = "Partially update a task (PATCH semantics — omitted fields are left unchanged). The \"type\" discriminator must match the existing task type.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Task updated successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Task not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{id}")
    public TaskResponse updateTask(
            @Parameter(description = "Task ID") @PathVariable Long id,
            @Valid @RequestBody TaskUpdateRequest request) {
        Task existingTask = taskService.getTaskForIdentity(requestContext.getIdentityId(), id);
        if (request.accountId() != null) {
            Account account = validateAccountOwnership(request.accountId());
            existingTask.setAccount(account);
        }

        return switch (request) {
            case StaticTaskUpdateRequest staticRequest -> {
                if (!(existingTask instanceof StaticTask staticTask)) {
                    throw new InvalidRequestException("Task type mismatch: expected static task");
                }
                taskMapper.fromUpdateRequest(staticRequest, staticTask);
                yield taskMapper.toResponse(taskService.updateStaticTask(id, staticTask));
            }
            case DynamicTaskUpdateRequest dynamicRequest -> {
                if (!(existingTask instanceof DynamicTask dynamicTask)) {
                    throw new InvalidRequestException("Task type mismatch: expected dynamic task");
                }
                taskMapper.fromUpdateRequest(dynamicRequest, dynamicTask);
                yield taskMapper.toResponse(taskService.updateDynamicTask(id, dynamicTask, dynamicRequest.dependencies()));
            }
        };
    }

    @Operation(summary = "Delete task", description = "Delete a task and all its associated labels, scopes and dependencies.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Task deleted"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Task not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(
            @Parameter(description = "Task ID") @PathVariable Long id) {
        taskService.deleteTask(requestContext.getIdentityId(), id);
    }
}


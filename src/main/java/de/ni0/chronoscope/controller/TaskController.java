package de.ni0.chronoscope.controller;

import java.util.List;

import de.ni0.chronoscope.model.*;
import de.ni0.chronoscope.service.KeycloakService;
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
import de.ni0.chronoscope.exception.InvalidRequestException;
import de.ni0.chronoscope.mapper.TaskMapper;
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

/**
 * REST controller for task CRUD operations and dynamic-task dependency updates.
 */
@Tag(name = "Tasks", description = "Create, read, update and delete tasks and their dependencies")
@RestController
@RequestMapping("/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final TaskMapper taskMapper;
    private final RequestContext requestContext;
    private final KeycloakService keycloakService;

    /**
     * Lists all tasks visible to the authenticated identity.
     *
     * @return task responses, preserving the static/dynamic discriminator
     */
    @Operation(summary = "List tasks", description = "Return all tasks belonging to the current identity, including tasks from linked accounts. Each task is either a StaticTask or a DynamicTask, discriminated by the \"type\" field.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })

    @GetMapping
    public List<TaskResponse> getTasks() {
        return taskService.getTasksForIdentity(requestContext.getAccount().getIdentity().getId()).stream()
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

    /**
     * Creates either a static or dynamic task based on the request discriminator.
     *
     * @param request polymorphic task creation payload
     * @return created task response
     */
    @Operation(summary = "Create task", description = "Create a new task. Set \"type\" to \"static\" for a StaticTask or \"dynamic\" for a DynamicTask with scheduling metadata.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Task created"),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Organization not linked to account", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody TaskCreateRequest request) {
        Identity identity = this.requestContext.getAccount().getIdentity();
        this.keycloakService.validateIdentityOrgAccess(identity, request.organizationId());
        TaskResponse response = switch (request) {
            case StaticTaskCreateRequest staticRequest -> {
                StaticTask newTask = taskMapper.fromCreateRequest(staticRequest);
                // Normalize rrule: treat null or blank as empty string (no rrule)
                String createRrule = staticRequest.rrule();
                if (createRrule == null || createRrule.isBlank()) {
                    newTask.setRrule("");
                } else {
                    newTask.setRrule(createRrule);
                }
                newTask.setIdentity(identity);
                newTask.setOrganizationId(staticRequest.organizationId());
                yield taskMapper.toResponse(taskService.createStaticTask(newTask));
            }
            case DynamicTaskCreateRequest dynamicRequest -> {
                DynamicTask newTask = taskMapper.fromCreateRequest(dynamicRequest);
                newTask.setIdentity(identity);
                newTask.setOrganizationId(dynamicRequest.organizationId());
                yield taskMapper.toResponse(taskService.createDynamicTask(newTask));
            }
        };
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves one task through the authenticated identity boundary.
     *
     * @param id task ID
     * @return matching task response
     */
    @Operation(summary = "Get task", description = "Retrieve a single task by ID, including its labels, scopes (dynamic tasks) and dependencies.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Task retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Task not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public TaskResponse getTask(
            @Parameter(description = "Task ID") @PathVariable Long id) {
        return mapTask(taskService.getTaskForIdentity(requestContext.getAccount().getIdentity().getId(), id));
    }

    /**
     * Applies a partial update to a static or dynamic task.
     *
     * @param id task ID
     * @param request polymorphic patch payload
     * @return updated task response
     */
    @Operation(summary = "Update task", description = "Partially update a task (PATCH semantics — omitted fields are left unchanged). The \"type\" discriminator must match the existing task type.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Task updated successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Organization not linked to account", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Task not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{id}")
    public TaskResponse updateTask(
            @Parameter(description = "Task ID") @PathVariable Long id,
            @Valid @RequestBody TaskUpdateRequest request) {
        Identity identity = this.requestContext.getAccount().getIdentity();
        this.keycloakService.validateIdentityOrgAccess(identity, request.organizationId());
        Task existingTask = taskService.getTaskForIdentity(requestContext.getAccount().getIdentity().getId(), id);

        return switch (request) {
            case StaticTaskUpdateRequest staticRequest -> {
                if (!(existingTask instanceof StaticTask staticTask)) {
                    throw new InvalidRequestException("Task type mismatch: expected static task");
                }
                taskMapper.fromUpdateRequest(staticRequest, staticTask);
                // Update semantics for rrule: omitted (null) -> leave unchanged; provided -> set (blank -> clear)
                if (staticRequest.rrule() != null) {
                    String updateRrule = staticRequest.rrule();
                    if (updateRrule.isBlank()) {
                        staticTask.setRrule("");
                    } else {
                        staticTask.setRrule(updateRrule);
                    }
                }
                yield taskMapper.toResponse(taskService.updateStaticTask(id, staticTask, staticRequest.organizationId()));
            }
            case DynamicTaskUpdateRequest dynamicRequest -> {
                if (!(existingTask instanceof DynamicTask dynamicTask)) {
                    throw new InvalidRequestException("Task type mismatch: expected dynamic task");
                }
                taskMapper.fromUpdateRequest(dynamicRequest, dynamicTask);
                yield taskMapper.toResponse(taskService.updateDynamicTask(id, dynamicTask, dynamicRequest.dependencies(), dynamicRequest.organizationId()));
            }
        };
    }

    /**
     * Deletes a task and its owned labels/scopes/dependency edges.
     *
     * @param id task ID
     */
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
        taskService.deleteTask(requestContext.getAccount().getIdentity().getId(), id);
    }
}

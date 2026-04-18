package de.ni0.chronoscope.controller;

import java.util.List;

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

import de.ni0.chronoscope.controller.dto.request.DynamicTaskCreateRequest;
import de.ni0.chronoscope.controller.dto.request.StaticTaskCreateRequest;
import de.ni0.chronoscope.controller.dto.request.TaskCreateRequest;
import de.ni0.chronoscope.controller.dto.request.TaskDependencyCreateRequest;
import de.ni0.chronoscope.controller.dto.request.TaskUpdateRequest;
import de.ni0.chronoscope.controller.dto.response.TaskDependencyResponse;
import de.ni0.chronoscope.controller.dto.response.TaskResponse;
import de.ni0.chronoscope.exception.ApiNotImplementedException;
import de.ni0.chronoscope.mapper.TaskMapper;
import de.ni0.chronoscope.model.DynamicTask;
import de.ni0.chronoscope.model.StaticTask;
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

    @Operation(summary = "List tasks", description = "Return all tasks belonging to the current identity. Each task is either a StaticTask or a DynamicTask, discriminated by the \"type\" field.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })

    @GetMapping
    public List<TaskResponse> getTasks() {
        throw new ApiNotImplementedException();
    }

    @Operation(summary = "Create task", description = "Create a new task. Set \"type\" to \"static\" for a StaticTask or \"dynamic\" for a DynamicTask with scheduling metadata.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Task created"),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody TaskCreateRequest request) {
        if(request instanceof StaticTaskCreateRequest staticRequest) {
            StaticTask newTask = taskMapper.fromCreateRequest(staticRequest);
            StaticTask createdTask = taskService.createStaticTask(newTask);
            TaskResponse response = taskMapper.toResponse(createdTask);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else if(request instanceof DynamicTaskCreateRequest dynamicRequest) {
            DynamicTask newTask = taskMapper.fromCreateRequest(dynamicRequest);
            DynamicTask createdTask = taskService.createDynamicTask(newTask);
            TaskResponse response = taskMapper.toResponse(createdTask);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            throw new IllegalArgumentException("Unknown task type");
        }
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
        throw new ApiNotImplementedException();
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
        throw new ApiNotImplementedException();
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
        throw new ApiNotImplementedException();
    }

    @Operation(summary = "Create dependency", description = "Add a predecessor dependency to a dynamic task. The task identified by {id} will depend on predecessorDynamicTaskId.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Dependency created"),
        @ApiResponse(responseCode = "400", description = "Validation error or cycle detected", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Task not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{id}/dependencies")
    public ResponseEntity<TaskDependencyResponse> createDependency(
            @Parameter(description = "Dynamic task ID that gains the dependency") @PathVariable Long id,
            @Valid @RequestBody TaskDependencyCreateRequest request) {
        throw new ApiNotImplementedException();
    }

    @Operation(summary = "Delete dependency", description = "Remove a dependency link from a dynamic task.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Dependency deleted"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Task or dependency not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{id}/dependencies/{linkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDependency(
            @Parameter(description = "Dynamic task ID") @PathVariable Long id,
            @Parameter(description = "Dependency link ID") @PathVariable Long linkId) {
        throw new ApiNotImplementedException();
    }
}


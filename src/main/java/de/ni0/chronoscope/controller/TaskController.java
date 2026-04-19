package de.ni0.chronoscope.controller;

import de.ni0.chronoscope.controller.dto.request.TaskCreateRequest;
import de.ni0.chronoscope.controller.dto.request.TaskDependencyCreateRequest;
import de.ni0.chronoscope.controller.dto.request.TaskUpdateRequest;
import de.ni0.chronoscope.controller.dto.response.TaskDependencyResponse;
import de.ni0.chronoscope.controller.dto.response.TaskResponse;
import de.ni0.chronoscope.exception.ApiNotImplementedException;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Tasks", description = "Create, read, update and delete tasks and their dependencies")
@RestController
@RequestMapping("/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

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
        throw new ApiNotImplementedException();
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


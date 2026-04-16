package de.ni0.chronoscope.controller;

import de.ni0.chronoscope.controller.dto.TaskDto;
import de.ni0.chronoscope.controller.dto.request.TaskCreateRequest;
import de.ni0.chronoscope.controller.dto.request.TaskDependencyCreateRequest;
import de.ni0.chronoscope.controller.dto.request.TaskUpdateRequest;
import de.ni0.chronoscope.controller.dto.response.TaskDependencyResponse;
import de.ni0.chronoscope.controller.dto.response.TaskResponse;
import de.ni0.chronoscope.exception.ApiNotImplementedException;
import de.ni0.chronoscope.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public List<TaskDto> getTasks() {
        throw new ApiNotImplementedException();
    }

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody TaskCreateRequest request) {
        throw new ApiNotImplementedException();
    }

    @GetMapping("/{id}")
    public TaskResponse getTask(@PathVariable Long id) {
        throw new ApiNotImplementedException();
    }

    @PutMapping("/{id}")
    public TaskResponse updateTask(@PathVariable Long id, @Valid @RequestBody TaskUpdateRequest request) {
        throw new ApiNotImplementedException();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable Long id) {
        throw new ApiNotImplementedException();
    }

    @PostMapping("/{id}/dependencies")
    public ResponseEntity<TaskDependencyResponse> createDependency(
            @PathVariable Long id,
            @Valid @RequestBody TaskDependencyCreateRequest request) {
        throw new ApiNotImplementedException();
    }

    @DeleteMapping("/{id}/dependencies/{linkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDependency(@PathVariable Long id, @PathVariable Long linkId) {
        throw new ApiNotImplementedException();
    }
}


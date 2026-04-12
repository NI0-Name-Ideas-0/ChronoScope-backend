package de.ni0.chronoscope.controller;

import de.ni0.chronoscope.controller.dto.CreateTaskRequest;
import de.ni0.chronoscope.controller.dto.CreateTaskResponse;
import de.ni0.chronoscope.model.Task;
import de.ni0.chronoscope.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;

    @PostMapping
    public CreateTaskResponse create(@Valid @RequestBody CreateTaskRequest request) {
        Task task = new Task(request.getName(), request.getDescription());
        task = this.taskService.createTask(task);
        return new CreateTaskResponse(task);
    }
}

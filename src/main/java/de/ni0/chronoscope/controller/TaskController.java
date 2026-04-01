package de.ni0.chronoscope.controller;

import de.ni0.chronoscope.controller.dto.CreateTaskRequest;
import de.ni0.chronoscope.controller.dto.CreateTaskResponse;
import de.ni0.chronoscope.model.Task;
import de.ni0.chronoscope.service.TaskService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/tasks")
public class TaskController {
    private TaskService taskService;

    @PostMapping("/")
    public CreateTaskResponse create(@RequestBody CreateTaskRequest request) {
        Task task = new Task(request.getName(), request.getDuration());
        task = this.taskService.createTask(task);
        return new CreateTaskResponse(task);
    }
}

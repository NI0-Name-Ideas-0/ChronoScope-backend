package de.ni0.chronoscope.controller;

import de.ni0.chronoscope.controller.dto.CreateTaskRequest;
import de.ni0.chronoscope.controller.dto.CreateTaskResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/tasks")
public class TaskController {
    @PostMapping("/")
    public CreateTaskResponse create(@RequestBody CreateTaskRequest request) {

    }
}

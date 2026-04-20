package de.ni0.chronoscope.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.ni0.chronoscope.controller.dto.request.DynamicTaskCreateRequest;
import de.ni0.chronoscope.controller.dto.request.StaticTaskCreateRequest;
import de.ni0.chronoscope.controller.dto.request.TaskCreateRequest;
import de.ni0.chronoscope.controller.dto.response.LabelResponse;
import de.ni0.chronoscope.controller.dto.response.TaskResponse;
import de.ni0.chronoscope.mapper.LabelMapper;
import de.ni0.chronoscope.mapper.TaskMapper;
import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.DynamicTask;
import de.ni0.chronoscope.model.Identity;
import de.ni0.chronoscope.model.StaticTask;
import de.ni0.chronoscope.repository.AccountRepository;
import de.ni0.chronoscope.repository.IdentityRepository;
import de.ni0.chronoscope.repository.LabelRepository;
import de.ni0.chronoscope.service.TaskService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/test")
public class TestController {

    private final IdentityRepository identityRepository;
    private final AccountRepository accountRepository;
    private final LabelRepository labelRepository;
    private final LabelMapper labelMapper;

    private final TaskMapper taskMapper;

    private final TaskService taskService;

    public TestController(
        IdentityRepository identityRepository,
        AccountRepository accountRepository,
        LabelRepository labelRepository,
        LabelMapper labelMapper,
        TaskMapper taskMapper,
        TaskService taskService
    ) {
        this.identityRepository = identityRepository;
        this.accountRepository = accountRepository;
        this.labelRepository = labelRepository;
        this.labelMapper = labelMapper;
        this.taskMapper = taskMapper;
        this.taskService = taskService;
    }

    @GetMapping
    public String testEndpoint() {
        return "Hello from ChronoScope! Test Test";
    }

    @GetMapping("/createIdentity")
    public String testEndpoint2() {
        var identity = identityRepository.save(new Identity());
        Account account = new Account();
        account.setIdentity(identity);
        accountRepository.save(account);
        return identity.getId() + " - " + account.getId();
    }

    @GetMapping("/labels")
    public List<LabelResponse> testEndpoint3() {
        return labelRepository.findAll().stream().map(labelMapper::toResponse).toList();
    }

    @PostMapping("/tasks")
    public ResponseEntity<TaskResponse> testCreateTask(@Valid @RequestBody TaskCreateRequest request) {
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
}

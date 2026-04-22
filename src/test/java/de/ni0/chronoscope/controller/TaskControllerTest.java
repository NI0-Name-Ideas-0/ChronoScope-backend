package de.ni0.chronoscope.controller;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import de.ni0.chronoscope.config.RequestContext;
import de.ni0.chronoscope.controller.dto.request.DynamicTaskCreateRequest;
import de.ni0.chronoscope.controller.dto.request.DynamicTaskUpdateRequest;
import de.ni0.chronoscope.controller.dto.request.StaticTaskCreateRequest;
import de.ni0.chronoscope.controller.dto.request.StaticTaskUpdateRequest;
import de.ni0.chronoscope.controller.dto.response.DynamicTaskResponse;
import de.ni0.chronoscope.controller.dto.response.StaticTaskResponse;
import de.ni0.chronoscope.controller.dto.response.TaskResponse;
import de.ni0.chronoscope.exception.AccountAccessDeniedException;
import de.ni0.chronoscope.exception.AccountNotFoundException;
import de.ni0.chronoscope.exception.InvalidRequestException;
import de.ni0.chronoscope.mapper.TaskMapper;
import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.DynamicTask;
import de.ni0.chronoscope.model.Identity;
import de.ni0.chronoscope.model.StaticTask;
import de.ni0.chronoscope.repository.AccountRepository;
import de.ni0.chronoscope.service.TaskService;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    @Mock
    private TaskService taskService;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private AccountRepository accountRepository;

    @Test
    void getTasks_UsesCurrentIdentityAndMapsPolymorphicResponses() {
        RequestContext requestContext = new RequestContext();
        requestContext.setIdentityId(99L);
        TaskController controller = new TaskController(taskService, taskMapper, accountRepository, requestContext);

        StaticTask staticTask = new StaticTask();
        staticTask.setName("Static task");

        DynamicTask dynamicTask = new DynamicTask();
        dynamicTask.setName("Dynamic task");

        StaticTaskResponse staticResponse = new StaticTaskResponse(
            1L,
            10L,
            "Static task",
            "desc",
            1,
            Instant.parse("2026-04-20T09:00:00Z"),
            Instant.parse("2026-04-20T10:00:00Z"),
            "FREQ=DAILY",
            List.of(),
            false
        );
        DynamicTaskResponse dynamicResponse = new DynamicTaskResponse(
            2L,
            11L,
            "Dynamic task",
            "desc",
            2,
            Instant.parse("2026-04-20T08:00:00Z"),
            Instant.parse("2026-04-22T18:00:00Z"),
            "FREQ=DAILY",
            List.of(),
            120,
            0,
            30,
            60,
            List.of(),
            List.of(),
            List.of()
        );

        when(taskService.getTasksForIdentity(99L)).thenReturn(List.of(staticTask, dynamicTask));
        when(taskMapper.toResponse(staticTask)).thenReturn(staticResponse);
        when(taskMapper.toResponse(dynamicTask)).thenReturn(dynamicResponse);

        List<TaskResponse> result = controller.getTasks();

        assertEquals(2, result.size());
        assertEquals(staticResponse, result.get(0));
        assertEquals(dynamicResponse, result.get(1));
        verify(taskService).getTasksForIdentity(99L);
        verify(taskMapper).toResponse(staticTask);
        verify(taskMapper).toResponse(dynamicTask);
    }

    @Test
    void createTask_Static_CreatesStaticTaskWhenAuthorized() {
        RequestContext requestContext = new RequestContext();
        requestContext.setIdentityId(99L);
        TaskController controller = new TaskController(taskService, taskMapper, accountRepository, requestContext);

        Account account = new Account();
        Identity identity = new Identity();
        identity.setId(99L);
        account.setId(10L);
        account.setIdentity(identity);

        StaticTaskCreateRequest request = new StaticTaskCreateRequest(
            10L,
            "Write report",
            "Prepare weekly summary",
            "FREQ=WEEKLY;BYDAY=MO",
            3,
            Instant.parse("2026-04-20T09:00:00Z"),
            Instant.parse("2026-04-20T10:00:00Z"),
            List.of(),
            false
        );

        StaticTask mappedTask = new StaticTask();
        mappedTask.setName("Write report");

        StaticTask savedTask = new StaticTask();
        savedTask.setId(123L);
        savedTask.setAccount(account);
        savedTask.setName("Write report");

        StaticTaskResponse expectedResponse = new StaticTaskResponse(
            123L,
            10L,
            "Write report",
            "Prepare weekly summary",
            3,
            Instant.parse("2026-04-20T09:00:00Z"),
            Instant.parse("2026-04-20T10:00:00Z"),
            "FREQ=WEEKLY;BYDAY=MO",
            List.of(),
            false
        );

        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(taskMapper.fromCreateRequest(request)).thenReturn(mappedTask);
        when(taskService.createStaticTask(any(StaticTask.class))).thenReturn(savedTask);
        when(taskMapper.toResponse(savedTask)).thenReturn(expectedResponse);

        ResponseEntity<TaskResponse> response = controller.createTask(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
        verify(accountRepository).findById(10L);
        verify(taskService).createStaticTask(any(StaticTask.class));
        verify(taskMapper).toResponse(savedTask);
        verifyNoMoreInteractions(taskMapper, taskService, accountRepository);
    }

    @Test
    void createTask_Dynamic_CreatesDynamicTaskWhenAuthorized() {
        RequestContext requestContext = new RequestContext();
        requestContext.setIdentityId(99L);
        TaskController controller = new TaskController(taskService, taskMapper, accountRepository, requestContext);

        Account account = new Account();
        Identity identity = new Identity();
        identity.setId(99L);
        account.setId(11L);
        account.setIdentity(identity);

        DynamicTaskCreateRequest request = new DynamicTaskCreateRequest(
            11L,
            "Implement API endpoint",
            "Create and test endpoint",
            "FREQ=DAILY",
            4,
            Instant.parse("2026-04-20T08:00:00Z"),
            Instant.parse("2026-04-25T18:00:00Z"),
            List.of(),
            240,
            30,
            120,
            List.of()
        );

        DynamicTask mappedTask = new DynamicTask();
        mappedTask.setName("Implement API endpoint");

        DynamicTask savedTask = new DynamicTask();
        savedTask.setId(124L);
        savedTask.setAccount(account);
        savedTask.setName("Implement API endpoint");

        DynamicTaskResponse expectedResponse = new DynamicTaskResponse(
            124L,
            11L,
            "Implement API endpoint",
            "Create and test endpoint",
            4,
            Instant.parse("2026-04-20T08:00:00Z"),
            Instant.parse("2026-04-25T18:00:00Z"),
            "FREQ=DAILY",
            List.of(),
            240,
            0,
            30,
            120,
            List.of(),
            List.of(),
            List.of()
        );

        when(accountRepository.findById(11L)).thenReturn(Optional.of(account));
        when(taskMapper.fromCreateRequest(request)).thenReturn(mappedTask);
        when(taskService.createDynamicTask(any(DynamicTask.class))).thenReturn(savedTask);
        when(taskMapper.toResponse(savedTask)).thenReturn(expectedResponse);

        ResponseEntity<TaskResponse> response = controller.createTask(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
        verify(accountRepository).findById(11L);
        verify(taskService).createDynamicTask(any(DynamicTask.class));
        verify(taskMapper).toResponse(savedTask);
        verifyNoMoreInteractions(taskMapper, taskService, accountRepository);
    }

    @Test
    void createTask_ThrowsWhenAccountNotFound() {
        RequestContext requestContext = new RequestContext();
        requestContext.setIdentityId(99L);
        TaskController controller = new TaskController(taskService, taskMapper, accountRepository, requestContext);

        StaticTaskCreateRequest request = new StaticTaskCreateRequest(
            12L,
            "Write report",
            "Prepare weekly summary",
            "FREQ=WEEKLY;BYDAY=MO",
            3,
            Instant.parse("2026-04-20T09:00:00Z"),
            Instant.parse("2026-04-20T10:00:00Z"),
            List.of(),
            false
        );

        when(accountRepository.findById(12L)).thenReturn(Optional.empty());

        AccountNotFoundException ignored = assertThrows(AccountNotFoundException.class, () -> controller.createTask(request));
        assertEquals(AccountNotFoundException.class, ignored.getClass());
        verify(accountRepository).findById(12L);
        verifyNoMoreInteractions(taskMapper, taskService, accountRepository);
    }

    @Test
    void createTask_ThrowsWhenAccountBelongsToDifferentIdentity() {
        RequestContext requestContext = new RequestContext();
        requestContext.setIdentityId(99L);
        TaskController controller = new TaskController(taskService, taskMapper, accountRepository, requestContext);

        Account account = new Account();
        Identity identity = new Identity();
        identity.setId(123L);
        account.setId(13L);
        account.setIdentity(identity);

        StaticTaskCreateRequest request = new StaticTaskCreateRequest(
            13L,
            "Write report",
            "Prepare weekly summary",
            "FREQ=WEEKLY;BYDAY=MO",
            3,
            Instant.parse("2026-04-20T09:00:00Z"),
            Instant.parse("2026-04-20T10:00:00Z"),
            List.of(),
            false
        );

        when(accountRepository.findById(13L)).thenReturn(Optional.of(account));

        AccountAccessDeniedException ignored = assertThrows(AccountAccessDeniedException.class, () -> controller.createTask(request));
        assertEquals(AccountAccessDeniedException.class, ignored.getClass());
        verify(accountRepository).findById(13L);
        verifyNoMoreInteractions(taskMapper, taskService, accountRepository);
    }

    @Test
    void deleteTask_DelegatesToServiceWithCurrentIdentity() {
        RequestContext requestContext = new RequestContext();
        requestContext.setIdentityId(99L);
        TaskController controller = new TaskController(taskService, taskMapper, accountRepository, requestContext);

        controller.deleteTask(123L);

        verify(taskService).deleteTask(99L, 123L);
        verifyNoMoreInteractions(taskService, taskMapper, accountRepository);
    }

    @Test
    void getTask_Dynamic_UsesIdentityScopedLookupAndReturnsMappedResponse() {
        RequestContext requestContext = new RequestContext();
        requestContext.setIdentityId(99L);
        TaskController controller = new TaskController(taskService, taskMapper, accountRepository, requestContext);

        DynamicTask dynamicTask = new DynamicTask();
        dynamicTask.setId(500L);

        DynamicTaskResponse expectedResponse = new DynamicTaskResponse(
            500L,
            11L,
            "Dynamic task",
            "desc",
            2,
            Instant.parse("2026-04-20T08:00:00Z"),
            Instant.parse("2026-04-22T18:00:00Z"),
            "FREQ=DAILY",
            List.of(),
            120,
            0,
            30,
            60,
            List.of(),
            List.of(42L),
            List.of(600L)
        );

        when(taskService.getTaskForIdentity(99L, 500L)).thenReturn(dynamicTask);
        when(taskMapper.toResponse(dynamicTask)).thenReturn(expectedResponse);

        TaskResponse result = controller.getTask(500L);

        assertEquals(expectedResponse, result);
        verify(taskService).getTaskForIdentity(99L, 500L);
        verify(taskMapper).toResponse(dynamicTask);
        verifyNoMoreInteractions(taskService, taskMapper, accountRepository);
    }

    @Test
    void updateTask_Static_UpdatesAndMapsResponse() {
        RequestContext requestContext = new RequestContext();
        requestContext.setIdentityId(99L);
        TaskController controller = new TaskController(taskService, taskMapper, accountRepository, requestContext);

        StaticTask existingTask = new StaticTask();
        existingTask.setId(200L);

        StaticTaskUpdateRequest request = new StaticTaskUpdateRequest(
            "Updated static task",
            "Updated description",
            "FREQ=WEEKLY",
            2,
            Instant.parse("2026-04-20T09:00:00Z"),
            Instant.parse("2026-04-20T10:00:00Z"),
            List.of(),
            true
        );

        StaticTaskResponse expectedResponse = new StaticTaskResponse(
            200L,
            10L,
            "Updated static task",
            "Updated description",
            2,
            Instant.parse("2026-04-20T09:00:00Z"),
            Instant.parse("2026-04-20T10:00:00Z"),
            "FREQ=WEEKLY",
            List.of(),
            true
        );

        when(taskService.getTaskForIdentity(99L, 200L)).thenReturn(existingTask);
        when(taskService.updateStaticTask(200L, existingTask)).thenReturn(existingTask);
        when(taskMapper.toResponse(existingTask)).thenReturn(expectedResponse);

        TaskResponse result = controller.updateTask(200L, request);

        assertEquals(expectedResponse, result);
        verify(taskService).getTaskForIdentity(99L, 200L);
        verify(taskMapper).fromUpdateRequest(request, existingTask);
        verify(taskService).updateStaticTask(200L, existingTask);
        verify(taskMapper).toResponse(existingTask);
        verifyNoMoreInteractions(taskService, taskMapper, accountRepository);
    }

    @Test
    void updateTask_ThrowsWhenTypeDoesNotMatchPersistedTask() {
        RequestContext requestContext = new RequestContext();
        requestContext.setIdentityId(99L);
        TaskController controller = new TaskController(taskService, taskMapper, accountRepository, requestContext);

        StaticTask existingTask = new StaticTask();
        existingTask.setId(300L);

        DynamicTaskUpdateRequest request = new DynamicTaskUpdateRequest(
            "Dynamic name",
            "Dynamic description",
            "FREQ=DAILY",
            2,
            Instant.parse("2026-04-20T09:00:00Z"),
            Instant.parse("2026-04-20T10:00:00Z"),
            List.of(),
            90,
            15,
            15,
            60,
            List.of()
        );

        when(taskService.getTaskForIdentity(99L, 300L)).thenReturn(existingTask);

        InvalidRequestException ignored = assertThrows(InvalidRequestException.class, () -> controller.updateTask(300L, request));
        assertEquals(InvalidRequestException.class, ignored.getClass());

        verify(taskService).getTaskForIdentity(99L, 300L);
        verifyNoMoreInteractions(taskService, taskMapper, accountRepository);
    }

    @Test
    void updateTask_ThrowsWhenStaticPayloadTargetsDynamicTask() {
        RequestContext requestContext = new RequestContext();
        requestContext.setIdentityId(99L);
        TaskController controller = new TaskController(taskService, taskMapper, accountRepository, requestContext);

        DynamicTask existingTask = new DynamicTask();
        existingTask.setId(301L);

        StaticTaskUpdateRequest request = new StaticTaskUpdateRequest(
            "Static name",
            "Static description",
            "FREQ=WEEKLY",
            2,
            Instant.parse("2026-04-20T09:00:00Z"),
            Instant.parse("2026-04-20T10:00:00Z"),
            List.of(),
            false
        );

        when(taskService.getTaskForIdentity(99L, 301L)).thenReturn(existingTask);

        InvalidRequestException ignored = assertThrows(InvalidRequestException.class, () -> controller.updateTask(301L, request));
        assertEquals(InvalidRequestException.class, ignored.getClass());

        verify(taskService).getTaskForIdentity(99L, 301L);
        verifyNoMoreInteractions(taskService, taskMapper, accountRepository);
    }
}

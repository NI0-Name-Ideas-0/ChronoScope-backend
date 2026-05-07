package de.ni0.chronoscope.controller;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.ni0.chronoscope.TestData;
import de.ni0.chronoscope.model.Task;
import de.ni0.chronoscope.service.KeycloakService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.mockito.Mock;
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
import de.ni0.chronoscope.exception.InvalidRequestException;
import de.ni0.chronoscope.mapper.TaskMapper;
import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.DynamicTask;
import de.ni0.chronoscope.model.StaticTask;
import de.ni0.chronoscope.service.AccountService;
import de.ni0.chronoscope.service.TaskService;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    private static final long IDENTITY_ID = 99L;
    private static final long ACCOUNT_ID = 100L;

    @Mock
    private TaskService taskService;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private AccountService accountService;

    @Mock
    private KeycloakService keycloakService;

    @Test
    void getTasks_UsesCurrentIdentityAndMapsPolymorphicResponses() {
        Account account = TestData.account(IDENTITY_ID, ACCOUNT_ID);
        RequestContext requestContext = new RequestContext();
        requestContext.setAccount(account);
        TaskController controller = new TaskController(taskService, taskMapper, requestContext, keycloakService);

        StaticTask staticTask = new StaticTask();
        staticTask.setName("Static task");

        DynamicTask dynamicTask = new DynamicTask();
        dynamicTask.setName("Dynamic task");

        StaticTaskResponse staticResponse = new StaticTaskResponse(
            1L,
            UUID.randomUUID().toString(),
            "Static task",
            "desc",
            Task.Difficulty.TRIVIAL,
            Instant.parse("2026-04-20T09:00:00Z"),
            Instant.parse("2026-04-20T10:00:00Z"),
            "FREQ=DAILY",
            List.of(),
            false
        );
        DynamicTaskResponse dynamicResponse = new DynamicTaskResponse(
            2L,
                UUID.randomUUID().toString(),
            "Dynamic task",
            "desc",
            Task.Difficulty.TRIVIAL,
            Instant.parse("2026-04-20T08:00:00Z"),
            Instant.parse("2026-04-22T18:00:00Z"),
            List.of(),
                Duration.ofMinutes(120),
                Duration.ofMinutes(0),
                Duration.ofMinutes(30),
                Duration.ofMinutes(60),
            List.of(),
            List.of(),
            List.of()
        );

        when(taskService.getTasksForIdentity(IDENTITY_ID)).thenReturn(List.of(staticTask, dynamicTask));
        when(taskMapper.toResponse(staticTask)).thenReturn(staticResponse);
        when(taskMapper.toResponse(dynamicTask)).thenReturn(dynamicResponse);

        List<TaskResponse> result = controller.getTasks();

        assertEquals(2, result.size());
        assertEquals(staticResponse, result.get(0));
        assertEquals(dynamicResponse, result.get(1));
        verify(taskService).getTasksForIdentity(IDENTITY_ID);
        verify(taskMapper).toResponse(staticTask);
        verify(taskMapper).toResponse(dynamicTask);
    }

    @Test
    void createTask_Static_CreatesStaticTaskWhenAuthorized() {
        Account account = TestData.account(IDENTITY_ID, ACCOUNT_ID);
        RequestContext requestContext = new RequestContext();
        requestContext.setAccount(account);
        TaskController controller = new TaskController(taskService, taskMapper, requestContext, keycloakService);

        StaticTaskCreateRequest request = new StaticTaskCreateRequest(
            UUID.randomUUID().toString(),
            "Write report",
            "Prepare weekly summary",
            "FREQ=WEEKLY;BYDAY=MO",
            Task.Difficulty.TRIVIAL,
            Instant.parse("2026-04-20T09:00:00Z"),
            Instant.parse("2026-04-20T10:00:00Z"),
            List.of(),
            false
        );

        StaticTask mappedTask = new StaticTask();
        mappedTask.setName("Write report");

        StaticTask savedTask = new StaticTask();
        savedTask.setId(123L);
        savedTask.setIdentity(account.getIdentity());
        savedTask.setName("Write report");

        StaticTaskResponse expectedResponse = new StaticTaskResponse(
            123L,
            request.organizationId(),
            "Write report",
            "Prepare weekly summary",
            Task.Difficulty.TRIVIAL,
            Instant.parse("2026-04-20T09:00:00Z"),
            Instant.parse("2026-04-20T10:00:00Z"),
            "FREQ=WEEKLY;BYDAY=MO",
            List.of(),
            false
        );

        when(taskMapper.fromCreateRequest(request)).thenReturn(mappedTask);
        when(taskService.createStaticTask(any(StaticTask.class))).thenReturn(savedTask);
        when(taskMapper.toResponse(savedTask)).thenReturn(expectedResponse);

        ResponseEntity<TaskResponse> response = controller.createTask(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
        verify(taskMapper).fromCreateRequest(request);
        verify(taskService).createStaticTask(mappedTask);
        verify(taskMapper).toResponse(savedTask);
        verifyNoMoreInteractions(taskMapper, taskService, accountService);
    }

    @Test
    void createTask_Static_AllowsBlockerWithoutOrganization() {
        Account account = TestData.account(IDENTITY_ID, ACCOUNT_ID);
        RequestContext requestContext = new RequestContext();
        requestContext.setAccount(account);
        TaskController controller = new TaskController(taskService, taskMapper, requestContext, keycloakService);

        StaticTaskCreateRequest request = new StaticTaskCreateRequest(
            null,
            "Maintenance window",
            "Time that should stay blocked",
            "FREQ=DAILY;COUNT=1",
            Task.Difficulty.TRIVIAL,
            Instant.parse("2026-04-20T09:00:00Z"),
            Instant.parse("2026-04-20T10:00:00Z"),
            List.of(),
            true
        );

        StaticTask mappedTask = new StaticTask();
        mappedTask.setName("Maintenance window");

        StaticTask savedTask = new StaticTask();
        savedTask.setId(123L);
        savedTask.setIdentity(account.getIdentity());
        savedTask.setName("Maintenance window");
        savedTask.setIsBlocker(true);

        StaticTaskResponse expectedResponse = new StaticTaskResponse(
            123L,
            null,
            "Maintenance window",
            "Time that should stay blocked",
            Task.Difficulty.TRIVIAL,
            Instant.parse("2026-04-20T09:00:00Z"),
            Instant.parse("2026-04-20T10:00:00Z"),
            "FREQ=DAILY;COUNT=1",
            List.of(),
            true
        );

        when(taskMapper.fromCreateRequest(request)).thenReturn(mappedTask);
        when(taskService.createStaticTask(any(StaticTask.class))).thenReturn(savedTask);
        when(taskMapper.toResponse(savedTask)).thenReturn(expectedResponse);

        ResponseEntity<TaskResponse> response = controller.createTask(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
        verify(taskMapper).fromCreateRequest(request);
        verify(taskService).createStaticTask(mappedTask);
        verify(taskMapper).toResponse(savedTask);
        verifyNoMoreInteractions(taskMapper, taskService, accountService);
    }

    @Test
    void createTask_Dynamic_CreatesDynamicTaskWhenAuthorized() {
        Account account = TestData.account(IDENTITY_ID, ACCOUNT_ID);
        RequestContext requestContext = new RequestContext();
        requestContext.setAccount(account);
        TaskController controller = new TaskController(taskService, taskMapper, requestContext, keycloakService);

        DynamicTaskCreateRequest request = new DynamicTaskCreateRequest(
            UUID.randomUUID().toString(),
            "Implement API endpoint",
            "Create and test endpoint",
            Task.Difficulty.TRIVIAL,
            Instant.parse("2026-04-20T08:00:00Z"),
            Instant.parse("2026-04-25T18:00:00Z"),
            List.of(),
                Duration.of(240, ChronoUnit.MINUTES),
                Duration.of(30, ChronoUnit.MINUTES),
                Duration.of(120, ChronoUnit.MINUTES),
            List.of()
        );

        DynamicTask mappedTask = new DynamicTask();
        mappedTask.setName("Implement API endpoint");

        DynamicTask savedTask = new DynamicTask();
        savedTask.setId(124L);
        savedTask.setIdentity(account.getIdentity());
        savedTask.setName("Implement API endpoint");

        DynamicTaskResponse expectedResponse = new DynamicTaskResponse(
            124L,
            request.organizationId(),
            "Implement API endpoint",
            "Create and test endpoint",
            Task.Difficulty.TRIVIAL,
            Instant.parse("2026-04-20T08:00:00Z"),
            Instant.parse("2026-04-25T18:00:00Z"),
            List.of(),
                Duration.of(240, ChronoUnit.MINUTES),
                Duration.of(0, ChronoUnit.MINUTES),
                Duration.of(30, ChronoUnit.MINUTES),
                Duration.of(120, ChronoUnit.MINUTES),
            List.of(),
            List.of(),
            List.of()
        );

        when(taskMapper.fromCreateRequest(request)).thenReturn(mappedTask);
        when(taskService.createDynamicTask(any(DynamicTask.class))).thenReturn(savedTask);
        when(taskMapper.toResponse(savedTask)).thenReturn(expectedResponse);

        ResponseEntity<TaskResponse> response = controller.createTask(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
        verify(taskMapper).fromCreateRequest(request);
        verify(taskService).createDynamicTask(mappedTask);
        verify(taskMapper).toResponse(savedTask);
        verifyNoMoreInteractions(taskMapper, taskService, accountService);
    }

    @Test
    void deleteTask_DelegatesToServiceWithCurrentIdentity() {
        Account account = TestData.account(IDENTITY_ID, ACCOUNT_ID);
        RequestContext requestContext = new RequestContext();
        requestContext.setAccount(account);
        TaskController controller = new TaskController(taskService, taskMapper, requestContext, keycloakService);

        controller.deleteTask(123L);

        verify(taskService).deleteTask(account.getIdentity().getId(), 123L);
        verifyNoMoreInteractions(taskService, taskMapper);
    }

    @Test
    void getTask_Dynamic_UsesIdentityScopedLookupAndReturnsMappedResponse() {
        Account account = TestData.account(IDENTITY_ID, ACCOUNT_ID);
        RequestContext requestContext = new RequestContext();
        requestContext.setAccount(account);
        TaskController controller = new TaskController(taskService, taskMapper, requestContext, keycloakService);

        DynamicTask dynamicTask = new DynamicTask();
        dynamicTask.setId(500L);

        DynamicTaskResponse expectedResponse = new DynamicTaskResponse(
            500L,
            UUID.randomUUID().toString(),
            "Dynamic task",
            "desc",
                Task.Difficulty.TRIVIAL,
            Instant.parse("2026-04-20T08:00:00Z"),
            Instant.parse("2026-04-22T18:00:00Z"),
            List.of(),
                Duration.of(120, ChronoUnit.MINUTES),
                Duration.of(0, ChronoUnit.MINUTES),
                Duration.of(30, ChronoUnit.MINUTES),
                Duration.of(60, ChronoUnit.MINUTES),
            List.of(),
            List.of(42L),
            List.of(600L)
        );

        when(taskService.getTaskForIdentity(account.getIdentity().getId(), 500L)).thenReturn(dynamicTask);
        when(taskMapper.toResponse(dynamicTask)).thenReturn(expectedResponse);

        TaskResponse result = controller.getTask(500L);

        assertEquals(expectedResponse, result);
        verify(taskService).getTaskForIdentity(account.getIdentity().getId(), 500L);
        verify(taskMapper).toResponse(dynamicTask);
        verifyNoMoreInteractions(taskService, taskMapper);
    }

    @Test
    void updateTask_Static_UpdatesAndMapsResponse() {
        Account account = TestData.account(IDENTITY_ID, ACCOUNT_ID);
        RequestContext requestContext = new RequestContext();
        requestContext.setAccount(account);
        TaskController controller = new TaskController(taskService, taskMapper, requestContext, keycloakService);

        StaticTask existingTask = new StaticTask();
        existingTask.setId(200L);

        StaticTaskUpdateRequest request = new StaticTaskUpdateRequest(
            null,
            "Updated static task",
            "Updated description",
            "FREQ=WEEKLY",
            Task.Difficulty.TRIVIAL,
            Instant.parse("2026-04-20T09:00:00Z"),
            Instant.parse("2026-04-20T10:00:00Z"),
            List.of(),
            true
        );

        StaticTaskResponse expectedResponse = new StaticTaskResponse(
            200L,
            UUID.randomUUID().toString(),
            "Updated static task",
            "Updated description",
                Task.Difficulty.TRIVIAL,
            Instant.parse("2026-04-20T09:00:00Z"),
            Instant.parse("2026-04-20T10:00:00Z"),
            "FREQ=WEEKLY",
            List.of(),
            true
        );

        when(taskService.getTaskForIdentity(account.getIdentity().getId(), 200L)).thenReturn(existingTask);
        when(taskService.updateStaticTask(200L, existingTask, null)).thenReturn(existingTask);
        when(taskMapper.toResponse(existingTask)).thenReturn(expectedResponse);

        TaskResponse result = controller.updateTask(200L, request);

        assertEquals(expectedResponse, result);
        verify(taskService).getTaskForIdentity(account.getIdentity().getId(), 200L);
        verify(taskMapper).fromUpdateRequest(request, existingTask);
        verify(taskService).updateStaticTask(200L, existingTask, null);
        verify(taskMapper).toResponse(existingTask);
        verifyNoMoreInteractions(taskService, taskMapper);
    }

    @Test
    void updateTask_ThrowsWhenTypeDoesNotMatchPersistedTask() {
        Account account = TestData.account(IDENTITY_ID, ACCOUNT_ID);
        RequestContext requestContext = new RequestContext();
        requestContext.setAccount(account);
        TaskController controller = new TaskController(taskService, taskMapper, requestContext, keycloakService);

        StaticTask existingTask = new StaticTask();
        existingTask.setId(300L);

        DynamicTaskUpdateRequest request = new DynamicTaskUpdateRequest(
            null,
            "Dynamic name",
            "Dynamic description",
                Task.Difficulty.TRIVIAL,
            Instant.parse("2026-04-20T09:00:00Z"),
            Instant.parse("2026-04-20T10:00:00Z"),
            List.of(),
                Duration.of(90, ChronoUnit.MINUTES),
                Duration.of(15, ChronoUnit.MINUTES),
                Duration.of(15, ChronoUnit.MINUTES),
                Duration.of(60, ChronoUnit.MINUTES),
            List.of()
        );

        when(taskService.getTaskForIdentity(account.getIdentity().getId(), 300L)).thenReturn(existingTask);

        InvalidRequestException ignored = assertThrows(InvalidRequestException.class, () -> controller.updateTask(300L, request));
        assertEquals(InvalidRequestException.class, ignored.getClass());

        verify(taskService).getTaskForIdentity(account.getIdentity().getId(), 300L);
        verifyNoMoreInteractions(taskService, taskMapper);
    }

    @Test
    void updateTask_ThrowsWhenStaticPayloadTargetsDynamicTask() {
        Account account = TestData.account(IDENTITY_ID, ACCOUNT_ID);
        RequestContext requestContext = new RequestContext();
        requestContext.setAccount(account);
        TaskController controller = new TaskController(taskService, taskMapper, requestContext, keycloakService);

        DynamicTask existingTask = new DynamicTask();
        existingTask.setId(301L);

        StaticTaskUpdateRequest request = new StaticTaskUpdateRequest(
            null,
            "Static name",
            "Static description",
            "FREQ=WEEKLY",
                Task.Difficulty.TRIVIAL,
            Instant.parse("2026-04-20T09:00:00Z"),
            Instant.parse("2026-04-20T10:00:00Z"),
            List.of(),
            false
        );

        when(taskService.getTaskForIdentity(account.getIdentity().getId(), 301L)).thenReturn(existingTask);

        InvalidRequestException ignored = assertThrows(InvalidRequestException.class, () -> controller.updateTask(301L, request));
        assertEquals(InvalidRequestException.class, ignored.getClass());

        verify(taskService).getTaskForIdentity(account.getIdentity().getId(), 301L);
        verifyNoMoreInteractions(taskService, taskMapper);
    }

    @Test
    void updateTask_Static_ChangesOrganizationWhenProvided() {
        Account account = TestData.account(IDENTITY_ID, ACCOUNT_ID);
        RequestContext requestContext = new RequestContext();
        requestContext.setAccount(account);
        TaskController controller = new TaskController(taskService, taskMapper, requestContext, keycloakService);

        String targetOrganization = UUID.randomUUID().toString();

        StaticTask existingTask = new StaticTask();
        existingTask.setId(400L);
        existingTask.setIdentity(account.getIdentity());

        StaticTask savedTask = new StaticTask();
        savedTask.setId(400L);
        savedTask.setIdentity(account.getIdentity());
        savedTask.setOrganizationId(targetOrganization);

        StaticTaskResponse expectedResponse = new StaticTaskResponse(
            400L,
            targetOrganization,
            "Updated static task",
            "Updated description",
            Task.Difficulty.TRIVIAL,
            Instant.parse("2026-04-20T09:00:00Z"),
            Instant.parse("2026-04-20T10:00:00Z"),
            "FREQ=WEEKLY",
            List.of(),
            true
        );

        StaticTaskUpdateRequest request = new StaticTaskUpdateRequest(
            targetOrganization,
            "Updated static task",
            "Updated description",
            "FREQ=WEEKLY",
                Task.Difficulty.TRIVIAL,
            Instant.parse("2026-04-20T09:00:00Z"),
            Instant.parse("2026-04-20T10:00:00Z"),
            List.of(),
            true
        );

        when(taskService.getTaskForIdentity(account.getIdentity().getId(), 400L)).thenReturn(existingTask);
        when(taskService.updateStaticTask(400L, existingTask, targetOrganization)).thenReturn(savedTask);
        when(taskMapper.toResponse(savedTask)).thenReturn(expectedResponse);

        TaskResponse result = controller.updateTask(400L, request);

        assertEquals(expectedResponse, result);
        verify(taskService).getTaskForIdentity(account.getIdentity().getId(), 400L);
        verify(taskMapper).fromUpdateRequest(request, existingTask);
        verify(taskService).updateStaticTask(400L, existingTask, targetOrganization);
        verify(taskMapper).toResponse(savedTask);
        verifyNoMoreInteractions(taskService, taskMapper);
    }


    @Test
    void updateTask_Static_ChangeOrg_OrgNotLinked_Throws() {
        Account account = TestData.account(IDENTITY_ID, ACCOUNT_ID);
        RequestContext requestContext = new RequestContext();
        requestContext.setAccount(account);
        TaskController controller = new TaskController(taskService, taskMapper, requestContext, keycloakService);

        StaticTaskUpdateRequest request = new StaticTaskUpdateRequest(
            UUID.randomUUID().toString(),
            "Updated static task",
            "Updated description",
            "FREQ=WEEKLY",
            Task.Difficulty.TRIVIAL,
            Instant.parse("2026-04-20T09:00:00Z"),
            Instant.parse("2026-04-20T10:00:00Z"),
            List.of(),
            false
        );

        doThrow(new AccountAccessDeniedException("")).when(keycloakService)
                .validateIdentityOrgAccess(account.getIdentity(), request.organizationId());

        AccountAccessDeniedException ignored = assertThrows(AccountAccessDeniedException.class, () -> controller.updateTask(700L, request));
        assertEquals(AccountAccessDeniedException.class, ignored.getClass());

        verifyNoMoreInteractions(taskService, taskMapper);
    }
}

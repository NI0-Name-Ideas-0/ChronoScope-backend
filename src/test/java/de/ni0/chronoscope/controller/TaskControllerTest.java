package de.ni0.chronoscope.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.ni0.chronoscope.config.RequestContext;
import de.ni0.chronoscope.controller.dto.response.DynamicTaskResponse;
import de.ni0.chronoscope.controller.dto.response.StaticTaskResponse;
import de.ni0.chronoscope.controller.dto.response.TaskResponse;
import de.ni0.chronoscope.mapper.TaskMapper;
import de.ni0.chronoscope.model.DynamicTask;
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
}

package de.ni0.chronoscope.service;

import de.ni0.chronoscope.TestData;
import de.ni0.chronoscope.exception.InsufficientSlotsException;
import de.ni0.chronoscope.exception.InvalidRequestException;
import de.ni0.chronoscope.model.DynamicTask;
import de.ni0.chronoscope.model.Identity;
import de.ni0.chronoscope.model.Scope;
import de.ni0.chronoscope.model.WorkSlot;
import de.ni0.chronoscope.repository.ScopeRepository;
import de.ni0.chronoscope.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanningServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ScopeRepository scopeRepository;

    @Mock
    private WorkSlotService workSlotService;

    @Mock
    private AccountService accountService;

    @Mock
    private KeycloakService keycloakService;

    @Test
    void planTasksForAccount_ReturnsEmptyList_WhenNoTasksForOrganization() {
        PlanningService service = new PlanningService(taskRepository, scopeRepository, workSlotService, accountService, keycloakService);
        String orgId = UUID.randomUUID().toString();
        Identity identity = TestData.identity(1);

        when(taskRepository.findDynamicTasksByIdentityIdAndOrganization(identity.getId(), orgId)).thenReturn(List.of());

        List<Scope> result = service.planTasksForIdentity(identity, orgId);

        assertTrue(result.isEmpty());
        verify(taskRepository).findDynamicTasksByIdentityIdAndOrganization(identity.getId(), orgId);
        verify(workSlotService, never()).getWorkSlotsForIdentity(anyLong());
        verify(workSlotService, never()).getWorkSlotsForIdentity(anyLong(), anyString());
        verify(scopeRepository, never()).deleteByDynamicTaskIdIn(anyList());
        verify(scopeRepository, never()).saveAll(anyList());
        verifyNoMoreInteractions(taskRepository, scopeRepository, workSlotService, accountService);
    }

    @Test
    void planTasksForAccount_ThrowsInsufficientSlotsException_WhenNoWorkSlotsAvailable() {
        PlanningService service = new PlanningService(taskRepository, scopeRepository, workSlotService, accountService, keycloakService);
        Identity identity = TestData.identity(1);
        String orgId = UUID.randomUUID().toString();

        when(taskRepository.findDynamicTasksByIdentityIdAndOrganization(identity.getId(), orgId))
                .thenReturn(List.of(buildTask(1L, Duration.ofHours(1))));
        when(workSlotService.getWorkSlotsForIdentity(identity.getId(), orgId)).thenReturn(List.of());

        assertThrows(InsufficientSlotsException.class, () -> service.planTasksForIdentity(identity, orgId));

        verify(scopeRepository, never()).deleteByDynamicTaskIdIn(anyList());
        verify(scopeRepository, never()).saveAll(anyList());
    }

    @Test
    void planTasksForAccount_ThrowsInvalidRequestException_WhenTasksFormACycle() {
        PlanningService service = new PlanningService(taskRepository, scopeRepository, workSlotService, accountService, keycloakService);
        Identity identity = TestData.identity(1);
        String orgId = UUID.randomUUID().toString();

        DynamicTask taskA = buildTask(1L, Duration.ofHours(1));
        DynamicTask taskB = buildTask(2L, Duration.ofHours(1));
        taskA.setDependencies(new HashSet<>(Set.of(taskB)));
        taskB.setDependencies(new HashSet<>(Set.of(taskA)));

        WorkSlot slot = buildWorkSlot(
                Instant.parse("2026-04-26T08:00:00Z"),
                Instant.parse("2026-04-26T18:00:00Z"));

        when(taskRepository.findDynamicTasksByIdentityIdAndOrganization(identity.getId(), orgId))
                .thenReturn(List.of(taskA, taskB));
        when(workSlotService.getWorkSlotsForIdentity(identity.getId(), orgId)).thenReturn(List.of(slot));

        assertThrows(InvalidRequestException.class, () -> service.planTasksForIdentity(identity, orgId));

        verify(scopeRepository, never()).deleteByDynamicTaskIdIn(anyList());
        verify(scopeRepository, never()).saveAll(anyList());
    }

    @Test
    void planTasksForAccount_ThrowsInsufficientSlotsException_WhenDeadlineCannotBeMet() {
        PlanningService service = new PlanningService(taskRepository, scopeRepository, workSlotService, accountService, keycloakService);
        Identity identity = TestData.identity(1);
        String orgId = UUID.randomUUID().toString();

        // Task needs 2 hours but its deadline is only 1 hour from slot start
        DynamicTask task = buildTask(1L, Duration.ofHours(2));
        task.setStartAt(Instant.parse("2026-04-26T06:00:00Z"));
        task.setEndAt(Instant.parse("2026-04-26T09:00:00Z")); // only 1h after slot start

        WorkSlot slot = buildWorkSlot(
                Instant.parse("2026-04-26T08:00:00Z"),
                Instant.parse("2026-04-26T18:00:00Z"));

        when(taskRepository.findDynamicTasksByIdentityIdAndOrganization(identity.getId(), orgId))
                .thenReturn(List.of(task));
        when(workSlotService.getWorkSlotsForIdentity(identity.getId(), orgId)).thenReturn(List.of(slot));

        assertThrows(InsufficientSlotsException.class, () -> service.planTasksForIdentity(identity, orgId));

        verify(scopeRepository, never()).deleteByDynamicTaskIdIn(anyList());
        verify(scopeRepository, never()).saveAll(anyList());
    }

    @Test
    void planTasksForAccount_DeletesOldScopesAndReturnsNewScopes_WhenPlanSucceeds() {
        PlanningService service = new PlanningService(taskRepository, scopeRepository, workSlotService, accountService, keycloakService);
        Identity identity = TestData.identity(1);
        String orgId = UUID.randomUUID().toString();

        DynamicTask task = buildTask(1L, Duration.ofHours(1));
        WorkSlot slot = buildWorkSlot(
                Instant.parse("2026-04-26T08:00:00Z"),
                Instant.parse("2026-04-26T18:00:00Z"));

        when(taskRepository.findDynamicTasksByIdentityIdAndOrganization(identity.getId(), orgId))
                .thenReturn(List.of(task));
        when(workSlotService.getWorkSlotsForIdentity(identity.getId(), orgId)).thenReturn(List.of(slot));
        when(scopeRepository.deleteByDynamicTaskIdIn(List.of(1L))).thenReturn(0L);
        when(scopeRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<Scope> result = service.planTasksForIdentity(identity, orgId);

        assertEquals(1, result.size());
        assertEquals(task, result.getFirst().getDynamicTask());
        verify(scopeRepository).deleteByDynamicTaskIdIn(List.of(1L));
        verify(scopeRepository).saveAll(result);
        verifyNoMoreInteractions(taskRepository, scopeRepository, workSlotService, accountService);
    }

    private static DynamicTask buildTask(long id, Duration duration) {
        DynamicTask task = new DynamicTask();
        task.setId(id);
        task.setDuration(duration);
        task.setElapsed(Duration.ZERO);
        task.setMinScopeDuration(Duration.ofMinutes(30));
        task.setMaxScopeDuration(Duration.ofHours(1));
        task.setStartAt(Instant.parse("2026-04-26T06:00:00Z"));
        task.setEndAt(Instant.parse("2026-04-26T20:00:00Z"));
        task.setDependencies(new HashSet<>());
        task.setDependents(new HashSet<>());
        task.setScopes(new ArrayList<>());
        return task;
    }

    private static WorkSlot buildWorkSlot(Instant startAt, Instant endAt) {
        WorkSlot slot = new WorkSlot();
        slot.setStartAt(startAt);
        slot.setEndAt(endAt);
        return slot;
    }
}

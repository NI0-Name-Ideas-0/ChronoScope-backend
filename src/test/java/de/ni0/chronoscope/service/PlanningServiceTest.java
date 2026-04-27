package de.ni0.chronoscope.service;

import de.ni0.chronoscope.exception.AccountAccessDeniedException;
import de.ni0.chronoscope.exception.InsufficientSlotsException;
import de.ni0.chronoscope.exception.InvalidRequestException;
import de.ni0.chronoscope.model.DynamicTask;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
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

    @Test
    void planTasksForAccount_ReturnsEmptyList_WhenNoTasksForOrganization() {
        PlanningService service = new PlanningService(taskRepository, scopeRepository, workSlotService, accountService);

        doNothing().when(accountService).validateAccountOrgAccess(10L, 20L);
        when(taskRepository.findDynamicTasksByAccountIdAndOrganizationId(10L, 20L)).thenReturn(List.of());

        List<Scope> result = service.planTasksForAccount(10L, 20L);

        assertTrue(result.isEmpty());
        verify(accountService).validateAccountOrgAccess(10L, 20L);
        verify(taskRepository).findDynamicTasksByAccountIdAndOrganizationId(10L, 20L);
        verify(workSlotService, never()).getWorkSlotsForAccount(anyLong());
        verify(scopeRepository, never()).deleteByDynamicTaskIdIn(anyList());
        verify(scopeRepository, never()).saveAll(anyList());
        verifyNoMoreInteractions(taskRepository, scopeRepository, workSlotService, accountService);
    }

    @Test
    void planTasksForAccount_PropagatesException_WhenOrganizationAccessDenied() {
        PlanningService service = new PlanningService(taskRepository, scopeRepository, workSlotService, accountService);

        doThrow(new AccountAccessDeniedException("no access"))
                .when(accountService).validateAccountOrgAccess(10L, 20L);

        assertThrows(AccountAccessDeniedException.class, () -> service.planTasksForAccount(10L, 20L));

        verify(accountService).validateAccountOrgAccess(10L, 20L);
        verify(taskRepository, never()).findDynamicTasksByAccountIdAndOrganizationId(anyLong(), anyLong());
        verifyNoMoreInteractions(taskRepository, scopeRepository, workSlotService, accountService);
    }

    @Test
    void planTasksForAccount_ThrowsInsufficientSlotsException_WhenNoWorkSlotsAvailable() {
        PlanningService service = new PlanningService(taskRepository, scopeRepository, workSlotService, accountService);

        doNothing().when(accountService).validateAccountOrgAccess(10L, 20L);
        when(taskRepository.findDynamicTasksByAccountIdAndOrganizationId(10L, 20L))
                .thenReturn(List.of(buildTask(1L, Duration.ofHours(1))));
        when(workSlotService.getWorkSlotsForAccount(10L)).thenReturn(List.of());

        assertThrows(InsufficientSlotsException.class, () -> service.planTasksForAccount(10L, 20L));

        verify(scopeRepository, never()).deleteByDynamicTaskIdIn(anyList());
        verify(scopeRepository, never()).saveAll(anyList());
    }

    @Test
    void planTasksForAccount_ThrowsInvalidRequestException_WhenTasksFormACycle() {
        PlanningService service = new PlanningService(taskRepository, scopeRepository, workSlotService, accountService);

        DynamicTask taskA = buildTask(1L, Duration.ofHours(1));
        DynamicTask taskB = buildTask(2L, Duration.ofHours(1));
        taskA.setDependencies(new HashSet<>(Set.of(taskB)));
        taskB.setDependencies(new HashSet<>(Set.of(taskA)));

        WorkSlot slot = buildWorkSlot(
                Instant.parse("2026-04-26T08:00:00Z"),
                Instant.parse("2026-04-26T18:00:00Z"));

        doNothing().when(accountService).validateAccountOrgAccess(10L, 20L);
        when(taskRepository.findDynamicTasksByAccountIdAndOrganizationId(10L, 20L))
                .thenReturn(List.of(taskA, taskB));
        when(workSlotService.getWorkSlotsForAccount(10L)).thenReturn(List.of(slot));

        assertThrows(InvalidRequestException.class, () -> service.planTasksForAccount(10L, 20L));

        verify(scopeRepository, never()).deleteByDynamicTaskIdIn(anyList());
        verify(scopeRepository, never()).saveAll(anyList());
    }

    @Test
    void planTasksForAccount_ThrowsInsufficientSlotsException_WhenDeadlineCannotBeMet() {
        PlanningService service = new PlanningService(taskRepository, scopeRepository, workSlotService, accountService);

        // Task needs 2 hours but its deadline is only 1 hour from slot start
        DynamicTask task = buildTask(1L, Duration.ofHours(2));
        task.setStartAt(Instant.parse("2026-04-26T06:00:00Z"));
        task.setEndAt(Instant.parse("2026-04-26T09:00:00Z")); // only 1h after slot start

        WorkSlot slot = buildWorkSlot(
                Instant.parse("2026-04-26T08:00:00Z"),
                Instant.parse("2026-04-26T18:00:00Z"));

        doNothing().when(accountService).validateAccountOrgAccess(10L, 20L);
        when(taskRepository.findDynamicTasksByAccountIdAndOrganizationId(10L, 20L))
                .thenReturn(List.of(task));
        when(workSlotService.getWorkSlotsForAccount(10L)).thenReturn(List.of(slot));

        assertThrows(InsufficientSlotsException.class, () -> service.planTasksForAccount(10L, 20L));

        verify(scopeRepository, never()).deleteByDynamicTaskIdIn(anyList());
        verify(scopeRepository, never()).saveAll(anyList());
    }

    @Test
    void planTasksForAccount_DeletesOldScopesAndReturnsNewScopes_WhenPlanSucceeds() {
        PlanningService service = new PlanningService(taskRepository, scopeRepository, workSlotService, accountService);

        DynamicTask task = buildTask(1L, Duration.ofHours(1));
        WorkSlot slot = buildWorkSlot(
                Instant.parse("2026-04-26T08:00:00Z"),
                Instant.parse("2026-04-26T18:00:00Z"));

        doNothing().when(accountService).validateAccountOrgAccess(10L, 20L);
        when(taskRepository.findDynamicTasksByAccountIdAndOrganizationId(10L, 20L))
                .thenReturn(List.of(task));
        when(workSlotService.getWorkSlotsForAccount(10L)).thenReturn(List.of(slot));
        when(scopeRepository.deleteByDynamicTaskIdIn(List.of(1L))).thenReturn(0L);
        when(scopeRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<Scope> result = service.planTasksForAccount(10L, 20L);

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

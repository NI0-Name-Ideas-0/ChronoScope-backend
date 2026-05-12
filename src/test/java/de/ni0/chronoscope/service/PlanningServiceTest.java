package de.ni0.chronoscope.service;

import de.ni0.chronoscope.algorithm.ConcreteWorkSlot;
import de.ni0.chronoscope.algorithm.WorkSlotExpander;
import de.ni0.chronoscope.TestData;
import de.ni0.chronoscope.exception.InsufficientSlotsException;
import de.ni0.chronoscope.exception.InvalidRequestException;
import de.ni0.chronoscope.model.*;
import de.ni0.chronoscope.repository.ScopeRepository;
import de.ni0.chronoscope.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.DayOfWeek;
import java.time.LocalTime;
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

    @Mock
    private WorkSlotExpander workSlotExpander;

    @Test
    void planTasksForAccount_ReturnsEmptyList_WhenNoTasksForOrganization() {
        PlanningService service = new PlanningService(taskRepository, scopeRepository, workSlotService, keycloakService, workSlotExpander);
        String orgId = UUID.randomUUID().toString();
        Identity identity = TestData.identity(1);

        when(taskRepository.findDynamicTasksByIdentityIdAndOrganizationId(identity.getId(), orgId)).thenReturn(List.of());

        List<Scope> result = service.planTasksForIdentity(identity, orgId);

        assertTrue(result.isEmpty());
        verify(taskRepository).findDynamicTasksByIdentityIdAndOrganizationId(identity.getId(), orgId);
        verify(workSlotService, never()).getWorkSlotsForIdentity(anyLong());
        verify(workSlotService, never()).getWorkSlotsForIdentity(anyLong(), anyString());
        verify(scopeRepository, never()).deleteAll(anyList());
        verify(scopeRepository, never()).saveAll(anyList());
        verifyNoMoreInteractions(taskRepository, scopeRepository, workSlotService, accountService);
    }

    @Test
    void planTasksForAccount_ThrowsInsufficientSlotsException_WhenNoWorkSlotsAvailable() {
        PlanningService service = new PlanningService(taskRepository, scopeRepository, workSlotService, keycloakService, workSlotExpander);
        Identity identity = TestData.identity(1);
        String orgId = UUID.randomUUID().toString();

        when(taskRepository.findDynamicTasksByIdentityIdAndOrganizationId(identity.getId(), orgId))
                .thenReturn(List.of(buildTask(1L, Duration.ofHours(1))));
        when(workSlotService.getWorkSlotsForIdentity(identity.getId(), orgId)).thenReturn(List.of());

        assertThrows(InsufficientSlotsException.class, () -> service.planTasksForIdentity(identity, orgId));

        verify(scopeRepository, never()).deleteAll(anyList());
        verify(scopeRepository, never()).saveAll(anyList());
    }

    @Test
    void planTasksForAccount_ThrowsInvalidRequestException_WhenTasksFormACycle() {
        PlanningService service = new PlanningService(taskRepository, scopeRepository, workSlotService, keycloakService, workSlotExpander);
        Identity identity = TestData.identity(1);
        String orgId = UUID.randomUUID().toString();

        DynamicTask taskA = buildTask(1L, Duration.ofHours(1));
        DynamicTask taskB = buildTask(2L, Duration.ofHours(1));
        taskA.setDependencies(new HashSet<>(Set.of(taskB)));
        taskB.setDependencies(new HashSet<>(Set.of(taskA)));

        WorkSlot slot = buildWorkSlot();

        when(taskRepository.findDynamicTasksByIdentityIdAndOrganizationId(identity.getId(), orgId))
                .thenReturn(List.of(taskA, taskB));
        when(workSlotService.getWorkSlotsForIdentity(identity.getId(), orgId)).thenReturn(List.of(slot));
        when(workSlotExpander.expand(eq(List.of(slot)), any(Instant.class), any(Instant.class)))
            .thenReturn(List.of(concreteSlot(Instant.parse("2026-04-26T08:00:00Z"), Instant.parse("2026-04-26T18:00:00Z"))));

        assertThrows(InvalidRequestException.class, () -> service.planTasksForIdentity(identity, orgId));

        verify(scopeRepository, never()).deleteAll(anyList());
        verify(scopeRepository, never()).saveAll(anyList());
    }

    @Test
    void planTasksForAccount_ThrowsInsufficientSlotsException_WhenDeadlineCannotBeMet() {
        PlanningService service = new PlanningService(taskRepository, scopeRepository, workSlotService, keycloakService, workSlotExpander);
        Identity identity = TestData.identity(1);
        String orgId = UUID.randomUUID().toString();

        // Task needs 2 hours but its deadline is only 1 hour from slot start
        DynamicTask task = buildTask(1L, Duration.ofHours(2));
        task.setStartAt(Instant.parse("2026-04-26T06:00:00Z"));
        task.setEndAt(Instant.parse("2026-04-26T09:00:00Z")); // only 1h after slot start

        WorkSlot slot = buildWorkSlot();

        when(taskRepository.findDynamicTasksByIdentityIdAndOrganizationId(identity.getId(), orgId))
                .thenReturn(List.of(task));
        when(workSlotService.getWorkSlotsForIdentity(identity.getId(), orgId)).thenReturn(List.of(slot));
        when(workSlotExpander.expand(eq(List.of(slot)), any(Instant.class), any(Instant.class)))
            .thenReturn(List.of(concreteSlot(Instant.parse("2026-04-26T08:00:00Z"), Instant.parse("2026-04-26T18:00:00Z"))));

        assertThrows(InsufficientSlotsException.class, () -> service.planTasksForIdentity(identity, orgId));

        verify(scopeRepository, never()).deleteAll(anyList());
        verify(scopeRepository, never()).saveAll(anyList());
    }

    @Test
    void planTasksForAccount_DeletesOldScopesAndReturnsNewScopes_WhenPlanSucceeds() {
        PlanningService service = new PlanningService(taskRepository, scopeRepository, workSlotService, keycloakService, workSlotExpander);
        Identity identity = TestData.identity(1);
        String orgId = UUID.randomUUID().toString();

        DynamicTask task = buildTask(1L, Duration.ofHours(1));
        WorkSlot slot = buildWorkSlot();
        Scope existingScope = new Scope(2L, task, Instant.MIN, Instant.MAX);

        when(taskRepository.findDynamicTasksByIdentityIdAndOrganizationId(identity.getId(), orgId))
                .thenReturn(List.of(task));
        when(scopeRepository.findActiveScope(identity.getId())).thenReturn(List.of());
        when(scopeRepository.getScopesByDynamicTaskOrganizationIdAndDynamicTaskIdentityId(orgId, identity.getId()))
                .thenReturn(Set.of(existingScope));
        when(workSlotService.getWorkSlotsForIdentity(identity.getId(), orgId)).thenReturn(List.of(slot));
        when(workSlotExpander.expand(eq(List.of(slot)), any(Instant.class), any(Instant.class)))
            .thenReturn(List.of(concreteSlot(Instant.parse("2026-04-26T08:00:00Z"), Instant.parse("2026-04-26T18:00:00Z"))));
        when(scopeRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<Scope> result = service.planTasksForIdentity(identity, orgId);

        assertEquals(1, result.size());
        assertEquals(task, result.getFirst().getDynamicTask());
        verify(scopeRepository).deleteAllInBatch(Set.of(existingScope));
        verify(scopeRepository).saveAll(result);
        verifyNoMoreInteractions(taskRepository, scopeRepository, workSlotService, accountService);
    }

    @Test
    void planTasksForAccount_DoNotDeleteActiveScope_WhenPlanSucceeds() {
        PlanningService service = new PlanningService(taskRepository, scopeRepository, workSlotService, keycloakService, workSlotExpander);
        Identity identity = TestData.identity(1);
        String orgId = UUID.randomUUID().toString();

        DynamicTask task = buildTask(1L, Duration.ofHours(2));
        WorkSlot slot = buildWorkSlot();
        Scope existingScope = new Scope(2L, task,
                Instant.parse("2026-04-26T07:30:00Z"),
                Instant.parse("2026-04-26T08:30:00Z"));

        when(taskRepository.findDynamicTasksByIdentityIdAndOrganizationId(identity.getId(), orgId))
                .thenReturn(List.of(task));
        when(scopeRepository.findActiveScope(identity.getId())).thenReturn(List.of(existingScope));
        when(scopeRepository.getScopesByDynamicTaskOrganizationIdAndDynamicTaskIdentityId(orgId, identity.getId()))
                .thenReturn(Set.of(existingScope));
        when(workSlotService.getWorkSlotsForIdentity(identity.getId(), orgId)).thenReturn(List.of(slot));
        when(workSlotExpander.expand(eq(List.of(slot)), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(concreteSlot(Instant.parse("2026-04-26T08:30:00Z"), Instant.parse("2026-04-26T18:00:00Z"))));
        when(scopeRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<Scope> result = service.planTasksForIdentity(identity, orgId);

        assertEquals(1, result.size());
        assertEquals(task, result.getFirst().getDynamicTask());
        assertEquals(existingScope.getEndAt(), result.getFirst().getStartAt());
        verify(scopeRepository).deleteAllInBatch(Set.of());
        verify(scopeRepository).saveAll(result);
        verifyNoMoreInteractions(taskRepository, scopeRepository, workSlotService, accountService);
    }

    @Test
    void planTasksForAccount_PlanDependency_WhenActiveScopeResultsInFinishedTask() {
        PlanningService service = new PlanningService(taskRepository, scopeRepository, workSlotService, keycloakService, workSlotExpander);
        Identity identity = TestData.identity(1);
        String orgId = UUID.randomUUID().toString();

        DynamicTask task = buildTask(1L, Duration.ofHours(1));
        DynamicTask dependentTask = buildTask(2L, Duration.ofHours(1));
        dependentTask.setDependencies(Set.of(task));
        task.setDependents(Set.of(dependentTask));

        WorkSlot slot = buildWorkSlot();
        Scope existingScope = new Scope(2L, task,
                Instant.parse("2026-04-26T07:30:00Z"),
                Instant.parse("2026-04-26T08:30:00Z"));

        when(taskRepository.findDynamicTasksByIdentityIdAndOrganizationId(identity.getId(), orgId))
                .thenReturn(List.of(task, dependentTask));
        when(scopeRepository.findActiveScope(identity.getId())).thenReturn(List.of(existingScope));
        when(scopeRepository.getScopesByDynamicTaskOrganizationIdAndDynamicTaskIdentityId(orgId, identity.getId()))
                .thenReturn(Set.of(existingScope));
        when(workSlotService.getWorkSlotsForIdentity(identity.getId(), orgId)).thenReturn(List.of(slot));
        when(workSlotExpander.expand(eq(List.of(slot)), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(concreteSlot(Instant.parse("2026-04-26T08:30:00Z"), Instant.parse("2026-04-26T18:00:00Z"))));
        when(scopeRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<Scope> result = service.planTasksForIdentity(identity, orgId);

        assertEquals(1, result.size());
        assertEquals(dependentTask, result.getFirst().getDynamicTask());
        assertEquals(existingScope.getEndAt(), result.getFirst().getStartAt());
        verify(scopeRepository).deleteAllInBatch(Set.of());
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
        task.setDifficulty(Task.Difficulty.TRIVIAL);
        return task;
    }

    private static WorkSlot buildWorkSlot() {
        WorkSlot slot = new WorkSlot();
        slot.setDayOfWeek(DayOfWeek.SUNDAY);
        slot.setStartTime(LocalTime.of(8, 0));
        slot.setEndTime(LocalTime.of(18, 0));
        return slot;
    }

    private static ConcreteWorkSlot concreteSlot(Instant startAt, Instant endAt) {
        return new ConcreteWorkSlot(startAt, endAt, startAt.toString());
    }
}

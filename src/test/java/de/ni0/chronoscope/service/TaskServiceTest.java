package de.ni0.chronoscope.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import de.ni0.chronoscope.exception.InvalidRequestException;
import de.ni0.chronoscope.exception.ResourceNotFoundException;
import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.DynamicTask;
import de.ni0.chronoscope.model.Identity;
import de.ni0.chronoscope.model.Label;
import de.ni0.chronoscope.model.StaticTask;
import de.ni0.chronoscope.model.Task;
import de.ni0.chronoscope.repository.TaskRepository;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private KeycloakService keycloakService;

    private static void populateValidStaticFields(StaticTask task) {
        task.setName("Valid static task");
        task.setDescription("Valid static description");
        task.setDifficulty(Task.Difficulty.TRIVIAL);
        task.setStartAt(Instant.parse("2026-04-20T09:00:00Z"));
        task.setEndAt(Instant.parse("2026-04-20T10:00:00Z"));
        task.setRrule("FREQ=DAILY");
        task.setOrganizationId(UUID.randomUUID().toString());
        task.setIsBlocker(false);
    }

    private static void populateValidDynamicFields(DynamicTask task) {
        task.setName("Valid dynamic task");
        task.setDescription("Valid dynamic description");
        task.setDifficulty(Task.Difficulty.TRIVIAL);
        task.setStartAt(Instant.parse("2026-04-20T09:00:00Z"));
        task.setEndAt(Instant.parse("2026-04-21T10:00:00Z"));
        task.setDuration(Duration.ofMinutes(60));
        task.setElapsed(Duration.ZERO);
        task.setMinScopeDuration(Duration.ofMinutes(30));
        task.setMaxScopeDuration(Duration.ofMinutes(40));
        task.setOrganizationId(UUID.randomUUID().toString());
    }

    @Test
    void getTasksForIdentity_ReturnsAllTasksFromLinkedAccounts() {
        TaskService taskService = new TaskService(taskRepository, keycloakService);
        long identityId = 42L;
        List<Task> expectedTasks = List.of();

        when(taskRepository.findByIdentityId(identityId)).thenReturn(expectedTasks);

        List<Task> result = taskService.getTasksForIdentity(identityId);

        assertEquals(expectedTasks, result);
        verify(taskRepository).findByIdentityId(identityId);
    }

    @Test
    void getTaskForIdentity_ReturnsTaskWhenFound() {
        TaskService taskService = new TaskService(taskRepository, keycloakService);
        long identityId = 42L;
        long taskId = 101L;
        Task expectedTask = new StaticTask();
        expectedTask.setId(taskId);

        when(taskRepository.findByIdAndIdentityId(taskId, identityId)).thenReturn(Optional.of(expectedTask));

        Task result = taskService.getTaskForIdentity(identityId, taskId);

        assertEquals(expectedTask, result);
        verify(taskRepository).findByIdAndIdentityId(taskId, identityId);
    }

    @Test
    void getTaskForIdentity_ThrowsWhenNotFound() {
        TaskService taskService = new TaskService(taskRepository, keycloakService);
        long identityId = 42L;
        long taskId = 101L;

        when(taskRepository.findByIdAndIdentityId(taskId, identityId)).thenReturn(Optional.empty());

        ResourceNotFoundException ignored = assertThrows(ResourceNotFoundException.class, () -> taskService.getTaskForIdentity(identityId, taskId));
        assertEquals(ResourceNotFoundException.class, ignored.getClass());

        verify(taskRepository).findByIdAndIdentityId(taskId, identityId);
    }

    @Test
    void deleteTask_DeletesStaticTaskByIdentityScopedLookup() {
        TaskService taskService = new TaskService(taskRepository, keycloakService);
        long identityId = 42L;
        long taskId = 10L;

        StaticTask task = new StaticTask();
        task.setId(taskId);

        when(taskRepository.findByIdAndIdentityId(taskId, identityId)).thenReturn(Optional.of(task));

        taskService.deleteTask(identityId, taskId);

        verify(taskRepository).findByIdAndIdentityId(taskId, identityId);
        verify(taskRepository, never()).findDependentsByDependencyIdAndIdentityId(taskId, identityId);
        verify(taskRepository).delete(task);
        verify(taskRepository).flush();
    }

    @Test
    void deleteTask_DeletesDynamicTaskAndClearsDependencyRelations() {
        TaskService taskService = new TaskService(taskRepository, keycloakService);
        long identityId = 42L;
        long taskId = 11L;

        DynamicTask task = new DynamicTask();
        task.setId(taskId);

        DynamicTask predecessor = new DynamicTask();
        predecessor.setId(1L);
        predecessor.setDependents(Set.of(task));

        DynamicTask dependent = new DynamicTask();
        dependent.setId(2L);
        dependent.setDependencies(Set.of(task));

        task.setDependencies(Set.of(predecessor));
        task.setDependents(Set.of(dependent));

        // use mutable sets to allow relation cleanup in service
        predecessor.setDependents(new java.util.HashSet<>(predecessor.getDependents()));
        dependent.setDependencies(new java.util.HashSet<>(dependent.getDependencies()));
        task.setDependencies(new java.util.HashSet<>(task.getDependencies()));
        task.setDependents(new java.util.HashSet<>(task.getDependents()));

        when(taskRepository.findByIdAndIdentityId(taskId, identityId)).thenReturn(Optional.of(task));
        when(taskRepository.findDependentsByDependencyIdAndIdentityId(taskId, identityId)).thenReturn(List.of(dependent));

        taskService.deleteTask(identityId, taskId);

        assertEquals(0, task.getDependencies().size());
        assertEquals(0, task.getDependents().size());
        assertEquals(0, predecessor.getDependents().size());
        assertEquals(0, dependent.getDependencies().size());
        verify(taskRepository).findDependentsByDependencyIdAndIdentityId(taskId, identityId);
        verify(taskRepository).delete(task);
        verify(taskRepository).flush();
    }

    @Test
    void deleteTask_ThrowsWhenTaskNotFoundForIdentity() {
        TaskService taskService = new TaskService(taskRepository, keycloakService);
        long identityId = 42L;
        long taskId = 999L;

        when(taskRepository.findByIdAndIdentityId(taskId, identityId)).thenReturn(Optional.empty());

        ResourceNotFoundException ignored = assertThrows(ResourceNotFoundException.class, () -> taskService.deleteTask(identityId, taskId));
        assertEquals(ResourceNotFoundException.class, ignored.getClass());

        verify(taskRepository).findByIdAndIdentityId(taskId, identityId);
        verify(taskRepository, never()).findDependentsByDependencyIdAndIdentityId(taskId, identityId);
        verify(taskRepository, never()).delete(org.mockito.ArgumentMatchers.any());
        verify(taskRepository, never()).flush();
    }

    @Test
    void createDynamicTask_AllowsDependenciesFromDifferentAccountWithSameIdentity() {
        TaskService taskService = new TaskService(taskRepository, keycloakService);

        Identity identity = new Identity();
        identity.setId(42L);

        DynamicTask newTask = new DynamicTask();
        newTask.setIdentity(identity);
        populateValidDynamicFields(newTask);

        DynamicTask dependency = new DynamicTask();
        dependency.setId(100L);
        newTask.setDependencies(new java.util.HashSet<>(Set.of(dependency)));

        Account dependencyAccount = new Account();
        dependencyAccount.setId(20L);
        dependencyAccount.setIdentity(identity);

        DynamicTask persistedDependency = new DynamicTask();
        persistedDependency.setId(100L);
        persistedDependency.setIdentity(dependencyAccount.getIdentity());

        when(taskRepository.findAllById(Set.of(100L))).thenReturn(List.of(persistedDependency));
        when(taskRepository.save(newTask)).thenReturn(newTask);

        DynamicTask result = taskService.createDynamicTask(newTask);

        assertEquals(newTask, result);
        verify(taskRepository).findAllById(Set.of(100L));
        verify(taskRepository).save(newTask);
    }

    @Test
    void createStaticTask_AllowsBlockerWithoutOrganization() {
        TaskService taskService = new TaskService(taskRepository, keycloakService);

        StaticTask newTask = new StaticTask();
        populateValidStaticFields(newTask);
        newTask.setIsBlocker(true);
        newTask.setOrganizationId(null);

        when(taskRepository.save(newTask)).thenReturn(newTask);

        StaticTask result = taskService.createStaticTask(newTask);

        assertEquals(newTask, result);
        verify(taskRepository).save(newTask);
    }

    @Test
    void createStaticTask_ThrowsWhenNonBlockerHasNoOrganization() {
        TaskService taskService = new TaskService(taskRepository, keycloakService);

        StaticTask newTask = new StaticTask();
        populateValidStaticFields(newTask);
        newTask.setIsBlocker(false);
        newTask.setOrganizationId(null);

        InvalidRequestException exception = assertThrows(
            InvalidRequestException.class,
            () -> taskService.createStaticTask(newTask)
        );

        assertEquals("organizationId is required unless isBlocker is true", exception.getMessage());
        verify(taskRepository, never()).save(newTask);
    }

    @Test
    void createDynamicTask_ThrowsWhenOrganizationMissing() {
        TaskService taskService = new TaskService(taskRepository, keycloakService);

        DynamicTask newTask = new DynamicTask();
        populateValidDynamicFields(newTask);
        newTask.setOrganizationId(null);

        InvalidRequestException exception = assertThrows(
            InvalidRequestException.class,
            () -> taskService.createDynamicTask(newTask)
        );

        assertEquals("organizationId must be provided", exception.getMessage());
        verify(taskRepository, never()).save(newTask);
    }

    @Test
    void createDynamicTask_ThrowsWhenDependencyBelongsToDifferentIdentity() {
        TaskService taskService = new TaskService(taskRepository, keycloakService);

        Identity sourceIdentity = new Identity();
        sourceIdentity.setId(42L);
        Identity dependencyIdentity = new Identity();
        dependencyIdentity.setId(99L);

        DynamicTask newTask = new DynamicTask();
        newTask.setIdentity(sourceIdentity);
        populateValidDynamicFields(newTask);

        DynamicTask dependencyReference = new DynamicTask();
        dependencyReference.setId(100L);
        newTask.setDependencies(new java.util.HashSet<>(Set.of(dependencyReference)));

        DynamicTask persistedDependency = new DynamicTask();
        persistedDependency.setId(100L);
        persistedDependency.setIdentity(dependencyIdentity);

        when(taskRepository.findAllById(Set.of(100L))).thenReturn(List.of(persistedDependency));

        InvalidRequestException exception = assertThrows(
            InvalidRequestException.class,
            () -> taskService.createDynamicTask(newTask)
        );

        assertEquals("Dependency task 100 must belong to the same identity", exception.getMessage());
        verify(taskRepository).findAllById(Set.of(100L));
        verify(taskRepository, never()).save(newTask);
    }

    @Test
    void updateStaticTask_ThrowsWhenPathIdDoesNotMatchTaskId() {
        TaskService taskService = new TaskService(taskRepository, keycloakService);

        StaticTask task = new StaticTask();
        task.setId(100L);

        InvalidRequestException exception = assertThrows(
            InvalidRequestException.class,
            () -> taskService.updateStaticTask(101L, task)
        );

        assertEquals("Task id in path does not match target task", exception.getMessage());
        verify(taskRepository, never()).save(task);
    }

    @Test
    void updateDynamicTask_ThrowsWhenPathIdDoesNotMatchTaskId() {
        TaskService taskService = new TaskService(taskRepository, keycloakService);

        DynamicTask task = new DynamicTask();
        task.setId(100L);

        InvalidRequestException exception = assertThrows(
            InvalidRequestException.class,
            () -> taskService.updateDynamicTask(101L, task)
        );

        assertEquals("Task id in path does not match target task", exception.getMessage());
        verify(taskRepository, never()).findById(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void updateDynamicTask_ThrowsWhenPersistedTaskIsStatic() {
        TaskService taskService = new TaskService(taskRepository, keycloakService);

        Identity identity = new Identity();
        identity.setId(42L);

        DynamicTask task = new DynamicTask();
        task.setId(200L);
        task.setIdentity(identity);
        populateValidDynamicFields(task);

        StaticTask persistedTask = new StaticTask();
        persistedTask.setId(200L);

        when(taskRepository.findById(200L)).thenReturn(Optional.of(persistedTask));

        InvalidRequestException exception = assertThrows(
            InvalidRequestException.class,
            () -> taskService.updateDynamicTask(200L, task)
        );

        assertEquals("Task type mismatch: expected dynamic task", exception.getMessage());
        verify(taskRepository, never()).flush();
    }

    @Test
    void updateDynamicTask_ChangesOrganizationAndWiresLabels() {
        TaskService taskService = new TaskService(taskRepository, keycloakService);

        Identity identity = new Identity();
        identity.setId(42L);

        Label label = new Label();
        label.setName("backend");

        DynamicTask task = new DynamicTask();
        task.setId(200L);
        task.setIdentity(identity);
        populateValidDynamicFields(task);
        task.setLabels(new java.util.ArrayList<>(List.of(label)));
        task.setScopes(new java.util.ArrayList<>());
        task.setDependencies(new java.util.HashSet<>());

        DynamicTask managedTask = new DynamicTask();
        managedTask.setId(200L);
        managedTask.setIdentity(identity);
        populateValidDynamicFields(managedTask);
        managedTask.setOrganizationId("old-org");
        managedTask.setLabels(new java.util.ArrayList<>());
        managedTask.setScopes(new java.util.ArrayList<>());
        managedTask.setDependencies(new java.util.HashSet<>());
        managedTask.setDependents(new java.util.HashSet<>());

        when(taskRepository.findById(200L)).thenReturn(Optional.of(managedTask));

        DynamicTask result = taskService.updateDynamicTask(200L, task, null, "new-org");

        assertEquals(managedTask, result);
        assertEquals("new-org", managedTask.getOrganizationId());
        assertSame(managedTask, managedTask.getLabels().getFirst().getTask());
        verify(keycloakService).validateIdentityOrgAccess(identity, "new-org");
        verify(taskRepository).flush();
    }

    @Test
    void updateStaticTask_ChangesOrganizationAndWiresLabels() {
        TaskService taskService = new TaskService(taskRepository, keycloakService);

        Identity identity = new Identity();
        identity.setId(42L);

        Label label = new Label();
        label.setName("ops");

        StaticTask task = new StaticTask();
        task.setId(300L);
        task.setIdentity(identity);
        populateValidStaticFields(task);
        task.setLabels(new java.util.ArrayList<>(List.of(label)));

        StaticTask managedTask = new StaticTask();
        managedTask.setId(300L);
        managedTask.setIdentity(identity);
        populateValidStaticFields(managedTask);
        managedTask.setOrganizationId("old-org");
        managedTask.setLabels(new java.util.ArrayList<>());

        when(taskRepository.findById(300L)).thenReturn(Optional.of(managedTask));

        StaticTask result = taskService.updateStaticTask(300L, task, "new-org");

        assertEquals(managedTask, result);
        assertEquals("new-org", managedTask.getOrganizationId());
        assertSame(managedTask, managedTask.getLabels().getFirst().getTask());
        verify(keycloakService).validateIdentityOrgAccess(identity, "new-org");
        verify(taskRepository).flush();
    }

    @Test
    void createDynamicTask_RejectsMissingOrNonDynamicDependencies() {
        TaskService taskService = new TaskService(taskRepository, keycloakService);

        Identity identity = new Identity();
        identity.setId(42L);

        DynamicTask missingDependencyTask = new DynamicTask();
        missingDependencyTask.setIdentity(identity);
        populateValidDynamicFields(missingDependencyTask);
        DynamicTask missingDependencyRef = new DynamicTask();
        missingDependencyRef.setId(404L);
        missingDependencyTask.setDependencies(new java.util.HashSet<>(Set.of(missingDependencyRef)));

        when(taskRepository.findAllById(Set.of(404L))).thenReturn(List.of());

        InvalidRequestException missingException = assertThrows(
            InvalidRequestException.class,
            () -> taskService.createDynamicTask(missingDependencyTask)
        );
        assertEquals("Dependency task not found: 404", missingException.getMessage());

        DynamicTask staticDependencyTask = new DynamicTask();
        staticDependencyTask.setIdentity(identity);
        populateValidDynamicFields(staticDependencyTask);
        DynamicTask staticDependencyRef = new DynamicTask();
        staticDependencyRef.setId(405L);
        staticDependencyTask.setDependencies(new java.util.HashSet<>(Set.of(staticDependencyRef)));

        StaticTask persistedStaticDependency = new StaticTask();
        persistedStaticDependency.setId(405L);
        persistedStaticDependency.setIdentity(identity);
        when(taskRepository.findAllById(Set.of(405L))).thenReturn(List.of(persistedStaticDependency));

        InvalidRequestException staticException = assertThrows(
            InvalidRequestException.class,
            () -> taskService.createDynamicTask(staticDependencyTask)
        );
        assertEquals("Dependency task must be a dynamic task: 405", staticException.getMessage());

        verify(taskRepository, never()).save(missingDependencyTask);
        verify(taskRepository, never()).save(staticDependencyTask);
    }

    @Test
    void updateDynamicTask_ValidatesDependenciesBeforeSave() {
        TaskService taskService = new TaskService(taskRepository, keycloakService);

        Identity identity = new Identity();
        identity.setId(42L);

        DynamicTask dependencyRef = new DynamicTask();
        dependencyRef.setId(500L);

        DynamicTask dependency = new DynamicTask();
        dependency.setId(500L);
        dependency.setIdentity(identity);

        DynamicTask task = new DynamicTask();
        task.setId(200L);
        task.setIdentity(identity);
        populateValidDynamicFields(task);
        task.setDependencies(new java.util.HashSet<>(Set.of(dependencyRef)));
        task.setLabels(new java.util.ArrayList<>());
        task.setScopes(new java.util.ArrayList<>());

        DynamicTask managedTask = new DynamicTask();
        managedTask.setId(200L);
        managedTask.setIdentity(identity);
        populateValidDynamicFields(managedTask);
        managedTask.setDependencies(new java.util.HashSet<>(Set.of(dependencyRef)));
        managedTask.setDependents(new java.util.HashSet<>());
        managedTask.setLabels(new java.util.ArrayList<>());
        managedTask.setScopes(new java.util.ArrayList<>());

        when(taskRepository.findById(200L)).thenReturn(Optional.of(managedTask));
        when(taskRepository.findAllById(Set.of(500L))).thenReturn(List.of(dependency));

        DynamicTask result = taskService.updateDynamicTask(200L, task);

        assertEquals(managedTask, result);
        verify(taskRepository).findById(200L);
        verify(taskRepository).findAllById(Set.of(500L));
        verify(taskRepository).flush();
    }

    @Test
    void updateStaticTask_PersistsAndFlushes() {
        TaskService taskService = new TaskService(taskRepository, keycloakService);

        StaticTask task = new StaticTask();
        task.setId(100L);
        populateValidStaticFields(task);
        task.setLabels(new java.util.ArrayList<>());

        StaticTask managedTask = new StaticTask();
        managedTask.setId(100L);
        populateValidStaticFields(managedTask);
        managedTask.setLabels(new java.util.ArrayList<>());

        when(taskRepository.findById(100L)).thenReturn(Optional.of(managedTask));

        StaticTask result = taskService.updateStaticTask(100L, task);

        assertEquals(managedTask, result);
        verify(taskRepository).findById(100L);
        verify(taskRepository).flush();
    }

    @Test
    void createDynamicTask_ThrowsWhenDependencyIdIsNull() {
        TaskService taskService = new TaskService(taskRepository, keycloakService);

        Identity identity = new Identity();
        identity.setId(42L);

        DynamicTask newTask = new DynamicTask();
        newTask.setIdentity(identity);
        populateValidDynamicFields(newTask);

        DynamicTask dependencyWithoutId = new DynamicTask();
        newTask.setDependencies(new java.util.HashSet<>(Set.of(dependencyWithoutId)));

        InvalidRequestException exception = assertThrows(
            InvalidRequestException.class,
            () -> taskService.createDynamicTask(newTask)
        );

        assertEquals("Dependency task id must be provided", exception.getMessage());
        verify(taskRepository, never()).findAllById(org.mockito.ArgumentMatchers.any());
        verify(taskRepository, never()).save(newTask);
    }

    @Test
    void createDynamicTask_CapsDurationsToDuration_WhenMinOrMaxExceedDuration() {
        TaskService taskService = new TaskService(taskRepository, keycloakService);

        Identity identity = new Identity();
        identity.setId(42L);

        DynamicTask newTask = new DynamicTask();
        newTask.setIdentity(identity);
        populateValidDynamicFields(newTask);
        newTask.setDuration(Duration.ofMinutes(15));
        newTask.setMinScopeDuration(Duration.ofMinutes(20));
        newTask.setMaxScopeDuration(Duration.ofMinutes(25));
        newTask.setDependencies(new java.util.HashSet<>());

        when(taskRepository.save(newTask)).thenReturn(newTask);

        DynamicTask result = taskService.createDynamicTask(newTask);

        assertEquals(newTask, result);
        assertEquals(Duration.ofMinutes(15), newTask.getMinScopeDuration());
        assertEquals(Duration.ofMinutes(15), newTask.getMaxScopeDuration());
        verify(taskRepository).save(newTask);
    }

    @Test
    void createDynamicTask_ThrowsWhenMinScopeDurationBelowTenWithoutCapping() {
        TaskService taskService = new TaskService(taskRepository, keycloakService);

        Identity identity = new Identity();
        identity.setId(42L);

        DynamicTask newTask = new DynamicTask();
        newTask.setIdentity(identity);
        populateValidDynamicFields(newTask);
        newTask.setDuration(Duration.ofMinutes(40));
        newTask.setMinScopeDuration(Duration.ofMinutes(9));
        newTask.setMaxScopeDuration(Duration.ofMinutes(20));
        newTask.setDependencies(new java.util.HashSet<>());

        InvalidRequestException exception = assertThrows(
            InvalidRequestException.class,
            () -> taskService.createDynamicTask(newTask)
        );

        assertEquals("minScopeDuration must be at least 10 minutes", exception.getMessage());
        verify(taskRepository, never()).save(newTask);
    }

    @Test
    void createDynamicTask_ThrowsWhenMaxScopeDurationExceedsFourHours() {
        TaskService taskService = new TaskService(taskRepository, keycloakService);

        Identity identity = new Identity();
        identity.setId(42L);

        DynamicTask newTask = new DynamicTask();
        newTask.setIdentity(identity);
        populateValidDynamicFields(newTask);
        newTask.setDuration(Duration.ofHours(8));
        newTask.setMinScopeDuration(Duration.ofMinutes(30));
        newTask.setMaxScopeDuration(Duration.ofHours(5));
        newTask.setDependencies(new java.util.HashSet<>());

        InvalidRequestException exception = assertThrows(
            InvalidRequestException.class,
            () -> taskService.createDynamicTask(newTask)
        );

        assertEquals("maxScopeDuration must be at most 4 hours", exception.getMessage());
        verify(taskRepository, never()).save(newTask);
    }

    @Test
    void updateStaticTask_ThrowsWhenStartAtIsNotBeforeEndAt() {
        TaskService taskService = new TaskService(taskRepository, keycloakService);

        StaticTask task = new StaticTask();
        task.setId(120L);
        populateValidStaticFields(task);
        task.setStartAt(Instant.parse("2026-04-20T11:00:00Z"));
        task.setEndAt(Instant.parse("2026-04-20T10:00:00Z"));
        task.setLabels(new java.util.ArrayList<>());

        StaticTask managedTask = new StaticTask();
        managedTask.setId(120L);
        populateValidStaticFields(managedTask);
        managedTask.setLabels(new java.util.ArrayList<>());

        when(taskRepository.findById(120L)).thenReturn(Optional.of(managedTask));

        InvalidRequestException exception = assertThrows(
            InvalidRequestException.class,
            () -> taskService.updateStaticTask(120L, task)
        );

        assertEquals("startAt must be before endAt", exception.getMessage());
        verify(taskRepository, never()).flush();
    }

    @Test
    void updateDynamicTask_CapsMaxScopeDurationWhenDurationIsReduced() {
        TaskService taskService = new TaskService(taskRepository, keycloakService);

        Identity identity = new Identity();
        identity.setId(42L);

        DynamicTask task = new DynamicTask();
        task.setId(220L);
        task.setIdentity(identity);
        populateValidDynamicFields(task);
        task.setDuration(Duration.ofMinutes(20));
        task.setMinScopeDuration(Duration.ofMinutes(15));
        task.setMaxScopeDuration(Duration.ofMinutes(60));
        task.setDependencies(new java.util.HashSet<>());
        task.setLabels(new java.util.ArrayList<>());
        task.setScopes(new java.util.ArrayList<>());

        DynamicTask managedTask = new DynamicTask();
        managedTask.setId(220L);
        managedTask.setIdentity(identity);
        populateValidDynamicFields(managedTask);
        managedTask.setDependencies(new java.util.HashSet<>());
        managedTask.setDependents(new java.util.HashSet<>());
        managedTask.setLabels(new java.util.ArrayList<>());
        managedTask.setScopes(new java.util.ArrayList<>());

        when(taskRepository.findById(220L)).thenReturn(Optional.of(managedTask));

        DynamicTask result = taskService.updateDynamicTask(220L, task);

        assertEquals(managedTask, result);
        assertEquals(Duration.ofMinutes(20), managedTask.getMaxScopeDuration());
        verify(taskRepository).flush();
    }
}

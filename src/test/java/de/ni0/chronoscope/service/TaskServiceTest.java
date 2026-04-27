package de.ni0.chronoscope.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.ni0.chronoscope.exception.InvalidRequestException;
import de.ni0.chronoscope.exception.ResourceNotFoundException;
import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.DynamicTask;
import de.ni0.chronoscope.model.Identity;
import de.ni0.chronoscope.model.Organization;
import de.ni0.chronoscope.model.StaticTask;
import de.ni0.chronoscope.model.Task;
import de.ni0.chronoscope.repository.TaskRepository;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private AccountService accountService;

    @Test
    void getTasksForIdentity_ReturnsAllTasksFromLinkedAccounts() {
        TaskService taskService = new TaskService(taskRepository, accountService);
        long identityId = 42L;
        List<Task> expectedTasks = List.of();

        when(taskRepository.findByAccountIdentityId(identityId)).thenReturn(expectedTasks);
        when(taskRepository.findDynamicTasksByAccountIdentityId(identityId)).thenReturn(List.of());

        List<Task> result = taskService.getTasksForIdentity(identityId);

        assertEquals(expectedTasks, result);
        verify(taskRepository).findByAccountIdentityId(identityId);
        verify(taskRepository).findDynamicTasksByAccountIdentityId(identityId);
    }

    @Test
    void getTaskForIdentity_ReturnsTaskWhenFound() {
        TaskService taskService = new TaskService(taskRepository, accountService);
        long identityId = 42L;
        long taskId = 101L;
        Task expectedTask = new StaticTask();
        expectedTask.setId(taskId);

        when(taskRepository.findByIdAndAccountIdentityId(taskId, identityId)).thenReturn(Optional.of(expectedTask));

        Task result = taskService.getTaskForIdentity(identityId, taskId);

        assertEquals(expectedTask, result);
        verify(taskRepository).findByIdAndAccountIdentityId(taskId, identityId);
    }

    @Test
    void getTaskForIdentity_ThrowsWhenNotFound() {
        TaskService taskService = new TaskService(taskRepository, accountService);
        long identityId = 42L;
        long taskId = 101L;

        when(taskRepository.findByIdAndAccountIdentityId(taskId, identityId)).thenReturn(Optional.empty());

        ResourceNotFoundException ignored = assertThrows(ResourceNotFoundException.class, () -> taskService.getTaskForIdentity(identityId, taskId));
        assertEquals(ResourceNotFoundException.class, ignored.getClass());

        verify(taskRepository).findByIdAndAccountIdentityId(taskId, identityId);
    }

    @Test
    void deleteTask_DeletesStaticTaskByIdentityScopedLookup() {
        TaskService taskService = new TaskService(taskRepository, accountService);
        long identityId = 42L;
        long taskId = 10L;

        StaticTask task = new StaticTask();
        task.setId(taskId);

        when(taskRepository.findByIdAndAccountIdentityId(taskId, identityId)).thenReturn(Optional.of(task));

        taskService.deleteTask(identityId, taskId);

        verify(taskRepository).findByIdAndAccountIdentityId(taskId, identityId);
        verify(taskRepository, never()).findDependentsByDependencyIdAndAccountIdentityId(taskId, identityId);
        verify(taskRepository).delete(task);
        verify(taskRepository).flush();
    }

    @Test
    void deleteTask_DeletesDynamicTaskAndClearsDependencyRelations() {
        TaskService taskService = new TaskService(taskRepository, accountService);
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

        when(taskRepository.findByIdAndAccountIdentityId(taskId, identityId)).thenReturn(Optional.of(task));
        when(taskRepository.findDependentsByDependencyIdAndAccountIdentityId(taskId, identityId)).thenReturn(List.of(dependent));

        taskService.deleteTask(identityId, taskId);

        assertEquals(0, task.getDependencies().size());
        assertEquals(0, task.getDependents().size());
        assertEquals(0, predecessor.getDependents().size());
        assertEquals(0, dependent.getDependencies().size());
        verify(taskRepository).findDependentsByDependencyIdAndAccountIdentityId(taskId, identityId);
        verify(taskRepository).delete(task);
        verify(taskRepository).flush();
    }

    @Test
    void deleteTask_ThrowsWhenTaskNotFoundForIdentity() {
        TaskService taskService = new TaskService(taskRepository, accountService);
        long identityId = 42L;
        long taskId = 999L;

        when(taskRepository.findByIdAndAccountIdentityId(taskId, identityId)).thenReturn(Optional.empty());

        ResourceNotFoundException ignored = assertThrows(ResourceNotFoundException.class, () -> taskService.deleteTask(identityId, taskId));
        assertEquals(ResourceNotFoundException.class, ignored.getClass());

        verify(taskRepository).findByIdAndAccountIdentityId(taskId, identityId);
        verify(taskRepository, never()).findDependentsByDependencyIdAndAccountIdentityId(taskId, identityId);
        verify(taskRepository, never()).delete(org.mockito.ArgumentMatchers.any());
        verify(taskRepository, never()).flush();
    }

    @Test
    void createDynamicTask_AllowsDependenciesFromDifferentAccountWithSameIdentity() {
        TaskService taskService = new TaskService(taskRepository, accountService);

        Identity identity = new Identity();
        identity.setId(42L);

        Account sourceAccount = new Account();
        sourceAccount.setId(10L);
        sourceAccount.setIdentity(identity);

        DynamicTask newTask = new DynamicTask();
        newTask.setAccount(sourceAccount);

        DynamicTask dependency = new DynamicTask();
        dependency.setId(100L);
        newTask.setDependencies(new java.util.HashSet<>(Set.of(dependency)));

        Account dependencyAccount = new Account();
        dependencyAccount.setId(20L);
        dependencyAccount.setIdentity(identity);

        DynamicTask persistedDependency = new DynamicTask();
        persistedDependency.setId(100L);
        persistedDependency.setAccount(dependencyAccount);

        when(taskRepository.findAllById(Set.of(100L))).thenReturn(List.of(persistedDependency));
        when(taskRepository.save(newTask)).thenReturn(newTask);

        DynamicTask result = taskService.createDynamicTask(newTask);

        assertEquals(newTask, result);
        verify(taskRepository).findAllById(Set.of(100L));
        verify(taskRepository).save(newTask);
    }

    @Test
    void createDynamicTask_ThrowsWhenDependencyBelongsToDifferentIdentity() {
        TaskService taskService = new TaskService(taskRepository, accountService);

        Identity sourceIdentity = new Identity();
        sourceIdentity.setId(42L);
        Identity dependencyIdentity = new Identity();
        dependencyIdentity.setId(99L);

        Account sourceAccount = new Account();
        sourceAccount.setId(10L);
        sourceAccount.setIdentity(sourceIdentity);

        Account dependencyAccount = new Account();
        dependencyAccount.setId(20L);
        dependencyAccount.setIdentity(dependencyIdentity);

        DynamicTask newTask = new DynamicTask();
        newTask.setAccount(sourceAccount);

        DynamicTask dependencyReference = new DynamicTask();
        dependencyReference.setId(100L);
        newTask.setDependencies(new java.util.HashSet<>(Set.of(dependencyReference)));

        DynamicTask persistedDependency = new DynamicTask();
        persistedDependency.setId(100L);
        persistedDependency.setAccount(dependencyAccount);

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
        TaskService taskService = new TaskService(taskRepository, accountService);

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
    void updateDynamicTask_ValidatesDependenciesBeforeSave() {
        TaskService taskService = new TaskService(taskRepository, accountService);

        Identity identity = new Identity();
        identity.setId(42L);

        Account account = new Account();
        account.setId(10L);
        account.setIdentity(identity);

        DynamicTask dependencyRef = new DynamicTask();
        dependencyRef.setId(500L);

        DynamicTask dependency = new DynamicTask();
        dependency.setId(500L);
        dependency.setAccount(account);

        DynamicTask task = new DynamicTask();
        task.setId(200L);
        task.setAccount(account);
        task.setDependencies(new java.util.HashSet<>(Set.of(dependencyRef)));
        task.setLabels(new java.util.ArrayList<>());
        task.setScopes(new java.util.ArrayList<>());

        DynamicTask managedTask = new DynamicTask();
        managedTask.setId(200L);
        managedTask.setAccount(account);
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
        TaskService taskService = new TaskService(taskRepository, accountService);

        StaticTask task = new StaticTask();
        task.setId(100L);
        task.setLabels(new java.util.ArrayList<>());

        StaticTask managedTask = new StaticTask();
        managedTask.setId(100L);
        managedTask.setLabels(new java.util.ArrayList<>());

        when(taskRepository.findById(100L)).thenReturn(Optional.of(managedTask));

        StaticTask result = taskService.updateStaticTask(100L, task);

        assertEquals(managedTask, result);
        verify(taskRepository).findById(100L);
        verify(taskRepository).flush();
    }

    @Test
    void updateStaticTask_ValidatesOrganizationBeforeAssigning() {
        TaskService taskService = new TaskService(taskRepository, accountService);

        Account account = new Account();
        account.setId(10L);

        Organization organization = new Organization();
        organization.setId(7L);

        StaticTask task = new StaticTask();
        task.setId(100L);
        task.setLabels(new java.util.ArrayList<>());

        StaticTask managedTask = new StaticTask();
        managedTask.setId(100L);
        managedTask.setAccount(account);
        managedTask.setLabels(new java.util.ArrayList<>());

        when(taskRepository.findById(100L)).thenReturn(Optional.of(managedTask));
        when(accountService.resolveOrganizationForAccount(10L, 7L)).thenReturn(organization);

        StaticTask result = taskService.updateStaticTask(100L, task, 7L);

        assertEquals(managedTask, result);
        assertEquals(organization, managedTask.getOrganization());
        verify(taskRepository).findById(100L);
        verify(accountService).resolveOrganizationForAccount(10L, 7L);
        verify(taskRepository).flush();
    }

    @Test
    void updateDynamicTask_ValidatesOrganizationBeforeAssigning() {
        TaskService taskService = new TaskService(taskRepository, accountService);

        Account account = new Account();
        account.setId(10L);

        Organization organization = new Organization();
        organization.setId(7L);

        DynamicTask task = new DynamicTask();
        task.setId(200L);
        task.setLabels(new java.util.ArrayList<>());
        task.setScopes(new java.util.ArrayList<>());

        DynamicTask managedTask = new DynamicTask();
        managedTask.setId(200L);
        managedTask.setAccount(account);
        managedTask.setDependencies(new java.util.HashSet<>());
        managedTask.setDependents(new java.util.HashSet<>());
        managedTask.setLabels(new java.util.ArrayList<>());
        managedTask.setScopes(new java.util.ArrayList<>());

        when(taskRepository.findById(200L)).thenReturn(Optional.of(managedTask));
        when(accountService.resolveOrganizationForAccount(10L, 7L)).thenReturn(organization);

        DynamicTask result = taskService.updateDynamicTask(200L, task, null, 7L);

        assertEquals(managedTask, result);
        assertEquals(organization, managedTask.getOrganization());
        verify(taskRepository).findById(200L);
        verify(accountService).resolveOrganizationForAccount(10L, 7L);
        verify(taskRepository).flush();
    }

    @Test
    void createDynamicTask_ThrowsWhenDependencyIdIsNull() {
        TaskService taskService = new TaskService(taskRepository, accountService);

        Identity identity = new Identity();
        identity.setId(42L);

        Account sourceAccount = new Account();
        sourceAccount.setId(10L);
        sourceAccount.setIdentity(identity);

        DynamicTask newTask = new DynamicTask();
        newTask.setAccount(sourceAccount);

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
}

package de.ni0.chronoscope.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import de.ni0.chronoscope.exception.ResourceNotFoundException;
import de.ni0.chronoscope.model.DynamicTask;
import de.ni0.chronoscope.model.StaticTask;
import de.ni0.chronoscope.model.Task;
import de.ni0.chronoscope.repository.TaskRepository;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Test
    void getTasksForIdentity_ReturnsAllTasksFromLinkedAccounts() {
        TaskService taskService = new TaskService(taskRepository);
        long identityId = 42L;
        List<Task> expectedTasks = List.of();

        when(taskRepository.findByAccountIdentityId(identityId)).thenReturn(expectedTasks);

        List<Task> result = taskService.getTasksForIdentity(identityId);

        assertEquals(expectedTasks, result);
        verify(taskRepository).findByAccountIdentityId(identityId);
    }

    @Test
    void getTaskForIdentity_ReturnsTaskWhenFound() {
        TaskService taskService = new TaskService(taskRepository);
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
        TaskService taskService = new TaskService(taskRepository);
        long identityId = 42L;
        long taskId = 101L;

        when(taskRepository.findByIdAndAccountIdentityId(taskId, identityId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.getTaskForIdentity(identityId, taskId));

        verify(taskRepository).findByIdAndAccountIdentityId(taskId, identityId);
    }

    @Test
    void deleteTask_DeletesStaticTaskByIdentityScopedLookup() {
        TaskService taskService = new TaskService(taskRepository);
        long identityId = 42L;
        long taskId = 10L;

        StaticTask task = new StaticTask();
        task.setId(taskId);

        when(taskRepository.findByIdAndAccountIdentityId(taskId, identityId)).thenReturn(Optional.of(task));

        taskService.deleteTask(identityId, taskId);

        verify(taskRepository).findByIdAndAccountIdentityId(taskId, identityId);
        verify(taskRepository, never()).findDynamicTasksByDependencyId(taskId);
        verify(taskRepository).delete(task);
        verify(taskRepository).flush();
    }

    @Test
    void deleteTask_DeletesDynamicTaskAndClearsDependencyRelations() {
        TaskService taskService = new TaskService(taskRepository);
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
        when(taskRepository.findDynamicTasksByDependencyId(taskId)).thenReturn(List.of(dependent));

        taskService.deleteTask(identityId, taskId);

        assertEquals(0, task.getDependencies().size());
        assertEquals(0, task.getDependents().size());
        assertEquals(0, predecessor.getDependents().size());
        assertEquals(0, dependent.getDependencies().size());
        verify(taskRepository).findDynamicTasksByDependencyId(taskId);
        verify(taskRepository).delete(task);
        verify(taskRepository).flush();
    }

    @Test
    void deleteTask_ThrowsWhenTaskNotFoundForIdentity() {
        TaskService taskService = new TaskService(taskRepository);
        long identityId = 42L;
        long taskId = 999L;

        when(taskRepository.findByIdAndAccountIdentityId(taskId, identityId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.deleteTask(identityId, taskId));

        verify(taskRepository).findByIdAndAccountIdentityId(taskId, identityId);
        verify(taskRepository, never()).findDynamicTasksByDependencyId(taskId);
        verify(taskRepository, never()).delete(org.mockito.ArgumentMatchers.any());
        verify(taskRepository, never()).flush();
    }
}

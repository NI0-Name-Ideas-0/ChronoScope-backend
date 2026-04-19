package de.ni0.chronoscope.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

        assertSame(expectedTasks, result);
        verify(taskRepository).findByAccountIdentityId(identityId);
    }
}

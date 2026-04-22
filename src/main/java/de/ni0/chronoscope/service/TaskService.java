package de.ni0.chronoscope.service;

import java.util.HashSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.ni0.chronoscope.exception.ResourceNotFoundException;
import de.ni0.chronoscope.model.DynamicTask;
import de.ni0.chronoscope.model.StaticTask;
import de.ni0.chronoscope.model.Task;
import de.ni0.chronoscope.repository.TaskRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    public StaticTask createStaticTask(StaticTask task) {
        return this.taskRepository.save(task);
    }

    public DynamicTask createDynamicTask(DynamicTask task) {
        return this.taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public List<Task> getTasksForIdentity(long identityId) {
        return this.taskRepository.findByAccountIdentityId(identityId);
    }

    @Transactional(readOnly = true)
    public Task getTaskForIdentity(long identityId, Long taskId) {
        return this.taskRepository.findByIdAndAccountIdentityId(taskId, identityId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
    }

    @Transactional
    public void deleteTask(long identityId, Long taskId) {
        Task task = this.taskRepository.findByIdAndAccountIdentityId(taskId, identityId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));

        if (task instanceof DynamicTask dynamicTask) {
            List<DynamicTask> dependents = this.taskRepository.findDynamicTasksByDependencyId(taskId);

            for (DynamicTask dependency : new HashSet<>(dynamicTask.getDependencies())) {
                dependency.getDependents().remove(dynamicTask);
            }

            for (DynamicTask dependent : dependents) {
                dependent.getDependencies().remove(dynamicTask);
            }

            dynamicTask.getDependencies().clear();
            dynamicTask.getDependents().clear();
        }

        this.taskRepository.delete(task);
        this.taskRepository.flush();
    }

    public DynamicTask updateDynamicTask(Long id, DynamicTask task) {
        //TODO: validate task (e.g. duration > 0, minScopeDuration <= maxScopeDuration, etc.)
        return this.taskRepository.save(task);
    }

    public StaticTask updateStaticTask(Long id, StaticTask task) {
        //TODO: validate task (e.g. startAt < endAt, etc.)
        return this.taskRepository.save(task);
    }
}

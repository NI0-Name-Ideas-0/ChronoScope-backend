package de.ni0.chronoscope.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.ni0.chronoscope.exception.InvalidRequestException;
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
        validateDependencyIdentity(task);
        return this.taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public List<Task> getTasksForIdentity(long identityId) {
        return this.taskRepository.findByAccountIdentityIdWithRelations(identityId);
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

    @Transactional
    public DynamicTask updateDynamicTask(Long id, DynamicTask task) {
        return updateDynamicTask(id, task, null);
    }

    @Transactional
    public DynamicTask updateDynamicTask(Long id, DynamicTask task, List<Long> dependencyIds) {
        //TODO: validate task (e.g. duration > 0, minScopeDuration <= maxScopeDuration, etc.)
        if (!Objects.equals(id, task.getId())) {
            throw new InvalidRequestException("Task id in path does not match target task");
        }

        Task persistedTask = this.taskRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));
        if (!(persistedTask instanceof DynamicTask managedTask)) {
            throw new InvalidRequestException("Task type mismatch: expected dynamic task");
        }

        managedTask.setName(task.getName());
        managedTask.setDescription(task.getDescription());
        managedTask.setDifficulty(task.getDifficulty());
        managedTask.setStartAt(task.getStartAt());
        managedTask.setEndAt(task.getEndAt());
        managedTask.setRrule(task.getRrule());
        managedTask.setDuration(task.getDuration());
        managedTask.setElapsed(task.getElapsed());
        managedTask.setMinScopeDuration(task.getMinScopeDuration());
        managedTask.setMaxScopeDuration(task.getMaxScopeDuration());

        if (task.getLabels() != null) {
            managedTask.setLabels(task.getLabels());
            for (var label : managedTask.getLabels()) {
                label.setTask(managedTask);
            }
        }

        if (dependencyIds != null) {
            Set<DynamicTask> previousDependencies = new HashSet<>(managedTask.getDependencies());
            Set<DynamicTask> updatedDependencies = resolveAndValidateDependencies(managedTask, dependencyIds);

            for (DynamicTask previousDependency : previousDependencies) {
                if (!updatedDependencies.contains(previousDependency)) {
                    previousDependency.getDependents().remove(managedTask);
                }
            }

            managedTask.getDependencies().clear();
            managedTask.getDependencies().addAll(updatedDependencies);

            for (DynamicTask currentDependency : updatedDependencies) {
                currentDependency.getDependents().add(managedTask);
            }
        }

        validateDependencyIdentity(managedTask);
        this.taskRepository.flush();
        return managedTask;
    }

    @Transactional
    public StaticTask updateStaticTask(Long id, StaticTask task) {
        //TODO: validate task (e.g. startAt < endAt, etc.)
        if (!Objects.equals(id, task.getId())) {
            throw new InvalidRequestException("Task id in path does not match target task");
        }

        Task persistedTask = this.taskRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));
        if (!(persistedTask instanceof StaticTask managedTask)) {
            throw new InvalidRequestException("Task type mismatch: expected static task");
        }

        managedTask.setName(task.getName());
        managedTask.setDescription(task.getDescription());
        managedTask.setDifficulty(task.getDifficulty());
        managedTask.setStartAt(task.getStartAt());
        managedTask.setEndAt(task.getEndAt());
        managedTask.setRrule(task.getRrule());
        managedTask.setIsBlocker(task.getIsBlocker());

        if (task.getLabels() != null) {
            managedTask.setLabels(task.getLabels());
            for (var label : managedTask.getLabels()) {
                label.setTask(managedTask);
            }
        }

        this.taskRepository.flush();
        return managedTask;
    }

    private Set<DynamicTask> resolveAndValidateDependencies(DynamicTask task, List<Long> dependencyIds) {
        Set<Long> dependencyIdSet = dependencyIds.stream()
            .collect(Collectors.toSet());

        if (dependencyIdSet.isEmpty()) {
            return new HashSet<>();
        }

        Long sourceIdentityId = task.getAccount() != null
            && task.getAccount().getIdentity() != null
            ? task.getAccount().getIdentity().getId()
            : null;

        if (sourceIdentityId == null) {
            throw new InvalidRequestException("Dynamic task account identity must be set");
        }

        Map<Long, Task> dependenciesById = this.taskRepository.findAllById(dependencyIdSet).stream()
            .collect(Collectors.toMap(Task::getId, Function.identity()));

        Set<DynamicTask> resolvedDependencies = new HashSet<>();

        for (Long dependencyId : dependencyIdSet) {
            Task dependencyTask = dependenciesById.get(dependencyId);
            if (!(dependencyTask instanceof DynamicTask dynamicDependency)) {
                throw new InvalidRequestException("Dependency task not found: " + dependencyId);
            }

            Long dependencyIdentityId = dynamicDependency.getAccount() != null
                && dynamicDependency.getAccount().getIdentity() != null
                ? dynamicDependency.getAccount().getIdentity().getId()
                : null;

            if (!Objects.equals(sourceIdentityId, dependencyIdentityId)) {
                throw new InvalidRequestException("Dependency task " + dependencyId + " must belong to the same identity");
            }

            resolvedDependencies.add(dynamicDependency);
        }

        return resolvedDependencies;
    }

    private void validateDependencyIdentity(DynamicTask task) {
        if (task.getDependencies() == null || task.getDependencies().isEmpty()) {
            return;
        }

        Set<Long> dependencyIds = task.getDependencies().stream()
            .map(Task::getId)
            .collect(Collectors.toSet());

        if (dependencyIds.isEmpty()) {
            return;
        }

        Long sourceIdentityId = task.getAccount() != null
            && task.getAccount().getIdentity() != null
            ? task.getAccount().getIdentity().getId()
            : null;

        if (sourceIdentityId == null) {
            throw new InvalidRequestException("Dynamic task account identity must be set");
        }

        Map<Long, Task> dependenciesById = this.taskRepository.findAllById(dependencyIds).stream()
            .collect(Collectors.toMap(Task::getId, Function.identity()));

        for (Long dependencyId : dependencyIds) {
            Task dependencyTask = dependenciesById.get(dependencyId);
            if (!(dependencyTask instanceof DynamicTask)) {
                throw new InvalidRequestException("Dependency task not found: " + dependencyId);
            }

            Long dependencyIdentityId = dependencyTask.getAccount() != null
                && dependencyTask.getAccount().getIdentity() != null
                ? dependencyTask.getAccount().getIdentity().getId()
                : null;

            if (!Objects.equals(sourceIdentityId, dependencyIdentityId)) {
                throw new InvalidRequestException("Dependency task " + dependencyId + " must belong to the same identity");
            }
        }
    }
}

package de.ni0.chronoscope.service;

import java.time.Duration;
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
import de.ni0.chronoscope.model.Organization;
import de.ni0.chronoscope.model.StaticTask;
import de.ni0.chronoscope.model.Task;
import de.ni0.chronoscope.repository.TaskRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskService {

    private static final int MIN_DIFFICULTY = 1;
    private static final int MAX_DIFFICULTY = 5;
    private static final Duration MIN_SCOPE_DURATION = Duration.ofMinutes(10);
    private static final Duration MAX_SCOPE_DURATION = Duration.ofMinutes(90);
    private static final Duration MIN_SCOPE_GAP = Duration.ofMinutes(5);

    private final TaskRepository taskRepository;
    private final AccountService accountService;

    public StaticTask createStaticTask(StaticTask task) {
        validateStaticTask(task);
        return this.taskRepository.save(task);
    }

    public DynamicTask createDynamicTask(DynamicTask task) {
        validateAndNormalizeDynamicTask(task);
        validateDependencyIdentity(task);
        return this.taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public List<Task> getTasksForIdentity(long identityId) {
        List<Task> tasks = this.taskRepository.findByAccountIdentityId(identityId);
        this.taskRepository.findDynamicTasksByAccountIdentityId(identityId);
        return tasks;
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
            List<DynamicTask> dependents = this.taskRepository.findDependentsByDependencyIdAndAccountIdentityId(taskId, identityId);

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
        return updateDynamicTask(id, task, null, null);
    }

    @Transactional
    public DynamicTask updateDynamicTask(Long id, DynamicTask task, List<Long> dependencyIds) {
        return updateDynamicTask(id, task, dependencyIds, null);
    }

    @Transactional
    public DynamicTask updateDynamicTask(Long id, DynamicTask task, List<Long> dependencyIds, Long organizationId) {
        //TODO: validate task (e.g. duration > 0, minScopeDuration <= maxScopeDuration, etc.)
        if (!Objects.equals(id, task.getId())) {
            throw new InvalidRequestException("Task id in path does not match target task");
        }

        Task persistedTask = this.taskRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));
        if (!(persistedTask instanceof DynamicTask managedTask)) {
            throw new InvalidRequestException("Task type mismatch: expected dynamic task");
        }

        Organization organization = validateOrganizationAccess(managedTask, organizationId);

        managedTask.setAccount(task.getAccount());
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
        if (organization != null) {
            managedTask.setOrganization(organization);
        }

        if (task.getLabels() != null) {
            managedTask.setLabels(task.getLabels());
            for (var label : managedTask.getLabels()) {
                label.setTask(managedTask);
            }
        }

        validateAndNormalizeDynamicTask(managedTask);

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
        return updateStaticTask(id, task, null);
    }

    @Transactional
    public StaticTask updateStaticTask(Long id, StaticTask task, Long organizationId) {
        //TODO: validate task (e.g. startAt < endAt, etc.)
        if (!Objects.equals(id, task.getId())) {
            throw new InvalidRequestException("Task id in path does not match target task");
        }

        Task persistedTask = this.taskRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));
        if (!(persistedTask instanceof StaticTask managedTask)) {
            throw new InvalidRequestException("Task type mismatch: expected static task");
        }

        Organization organization = validateOrganizationAccess(managedTask, organizationId);

        managedTask.setAccount(task.getAccount());
        managedTask.setName(task.getName());
        managedTask.setDescription(task.getDescription());
        managedTask.setDifficulty(task.getDifficulty());
        managedTask.setStartAt(task.getStartAt());
        managedTask.setEndAt(task.getEndAt());
        managedTask.setRrule(task.getRrule());
        managedTask.setIsBlocker(task.getIsBlocker());
        if (organization != null) {
            managedTask.setOrganization(organization);
        }

        if (task.getLabels() != null) {
            managedTask.setLabels(task.getLabels());
            for (var label : managedTask.getLabels()) {
                label.setTask(managedTask);
            }
        }

        validateStaticTask(managedTask);

        this.taskRepository.flush();
        return managedTask;
    }

    private Organization validateOrganizationAccess(Task task, Long organizationId) {
        if (organizationId == null) {
            return null;
        }
        return this.accountService.resolveOrganizationForAccount(task.getAccount().getId(), organizationId);
    }

    private void validateStaticTask(StaticTask task) {
        validateCommonTaskFields(task);
    }

    private void validateAndNormalizeDynamicTask(DynamicTask task) {
        validateCommonTaskFields(task);

        Duration duration = task.getDuration();
        Duration minScopeDuration = task.getMinScopeDuration();
        Duration maxScopeDuration = task.getMaxScopeDuration();

        if (duration == null) {
            throw new InvalidRequestException("duration must be provided");
        }
        if (duration.isZero() || duration.isNegative()) {
            throw new InvalidRequestException("duration must be greater than 0");
        }
        if (minScopeDuration == null || maxScopeDuration == null) {
            throw new InvalidRequestException("minScopeDuration and maxScopeDuration must be provided");
        }

        boolean minScopeWasCapped = false;
        boolean maxScopeWasCapped = false;

        if (maxScopeDuration.compareTo(duration) > 0) {
            maxScopeDuration = duration;
            task.setMaxScopeDuration(maxScopeDuration);
            maxScopeWasCapped = true;
        }

        if (minScopeDuration.compareTo(duration) > 0) {
            minScopeDuration = duration;
            task.setMinScopeDuration(minScopeDuration);
            minScopeWasCapped = true;
        }

        if (maxScopeDuration.compareTo(MAX_SCOPE_DURATION) > 0) {
            throw new InvalidRequestException("maxScopeDuration must be at most 90 minutes");
        }

        if (minScopeDuration.compareTo(duration) > 0) {
            throw new InvalidRequestException("minScopeDuration must be less than or equal to duration");
        }

        if (!minScopeWasCapped && minScopeDuration.compareTo(MIN_SCOPE_DURATION) < 0) {
            throw new InvalidRequestException("minScopeDuration must be at least 10 minutes");
        }

        boolean allowReducedGap = minScopeWasCapped || maxScopeWasCapped;
        if (allowReducedGap) {
            if (maxScopeDuration.compareTo(minScopeDuration) < 0) {
                throw new InvalidRequestException("maxScopeDuration must be greater than or equal to minScopeDuration");
            }
            return;
        }

        if (maxScopeDuration.compareTo(minScopeDuration) <= 0) {
            throw new InvalidRequestException("maxScopeDuration must be greater than minScopeDuration");
        }

        if (maxScopeDuration.compareTo(minScopeDuration.plus(MIN_SCOPE_GAP)) < 0) {
            throw new InvalidRequestException("maxScopeDuration must be at least 5 minutes greater than minScopeDuration");
        }
    }

    private void validateCommonTaskFields(Task task) {
        if (task.getDifficulty() == null || task.getDifficulty() < MIN_DIFFICULTY || task.getDifficulty() > MAX_DIFFICULTY) {
            throw new InvalidRequestException("difficulty must be between 1 and 5");
        }

        if (task.getStartAt() == null || task.getEndAt() == null || !task.getStartAt().isBefore(task.getEndAt())) {
            throw new InvalidRequestException("startAt must be before endAt");
        }
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

        Set<Long> dependencyIds = new HashSet<>();
        for (Task dependency : task.getDependencies()) {
            if (dependency == null || dependency.getId() == null) {
                throw new InvalidRequestException("Dependency task id must be provided");
            }
            dependencyIds.add(dependency.getId());
        }

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
            if (dependencyTask == null) {
                throw new InvalidRequestException("Dependency task not found: " + dependencyId);
            }

            if (!(dependencyTask instanceof DynamicTask)) {
                throw new InvalidRequestException("Dependency task must be a dynamic task: " + dependencyId);
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

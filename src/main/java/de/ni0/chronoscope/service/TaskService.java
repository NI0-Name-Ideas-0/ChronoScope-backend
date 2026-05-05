package de.ni0.chronoscope.service;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.ni0.chronoscope.exception.AccountAccessDeniedException;
import de.ni0.chronoscope.model.Account;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.ni0.chronoscope.exception.InvalidRequestException;
import de.ni0.chronoscope.exception.ResourceNotFoundException;
import de.ni0.chronoscope.model.DynamicTask;
import de.ni0.chronoscope.model.StaticTask;
import de.ni0.chronoscope.model.Task;
import de.ni0.chronoscope.repository.TaskRepository;
import lombok.RequiredArgsConstructor;

/**
 * Manages task lifecycle, validation, and dynamic-task dependency integrity.
 */
@Service
@RequiredArgsConstructor
public class TaskService {

    private static final int MIN_DIFFICULTY = 1;
    private static final int MAX_DIFFICULTY = 5;
    private static final Duration MIN_SCOPE_DURATION = Duration.ofMinutes(10);
    private static final Duration MAX_SCOPE_DURATION = Duration.ofMinutes(90);
    private static final Duration MIN_SCOPE_GAP = Duration.ofMinutes(5);

    private final TaskRepository taskRepository;
    private final KeycloakService keycloakService;
    private final AccountService accountService;

    /**
     * Validates and persists a fixed-time task.
     *
     * @param task task to create
     * @return persisted task
     */
    public StaticTask createStaticTask(StaticTask task) {
        validateStaticTask(task);
        return this.taskRepository.save(task);
    }

    /**
     * Validates and persists a schedulable task.
     *
     * @param task task to create
     * @return persisted task
     */
    public DynamicTask createDynamicTask(DynamicTask task) {
        validateAndNormalizeDynamicTask(task);
        validateDependencyIdentity(task);
        return this.taskRepository.save(task);
    }

    /**
     * Returns all tasks visible to an identity, including tasks owned by linked accounts.
     *
     * @param identityId identity whose tasks should be loaded
     * @return visible tasks
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksForIdentity(long identityId) {
        List<Task> tasks = this.taskRepository.findByAccountIdentityId(identityId);
        this.taskRepository.findDynamicTasksByAccountIdentityId(identityId);
        return tasks;
    }

    /**
     * Loads one task through the identity boundary.
     *
     * @param identityId authenticated identity ID
     * @param taskId task ID
     * @return matching task
     */
    @Transactional(readOnly = true)
    public Task getTaskForIdentity(long identityId, Long taskId) {
        return this.taskRepository.findByIdAndAccountIdentityId(taskId, identityId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
    }

    /**
     * Deletes a task and detaches any dynamic-task dependency edges first.
     *
     * @param identityId authenticated identity ID
     * @param taskId task to delete
     */
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

    /**
     * Updates a dynamic task without changing dependencies or organization.
     *
     * @param id task ID from the request path
     * @param task merged task data
     * @return managed updated task
     */
    @Transactional
    public DynamicTask updateDynamicTask(Long id, DynamicTask task) {
        return updateDynamicTask(id, task, null, null);
    }

    /**
     * Updates a dynamic task and optionally replaces its dependency set.
     *
     * @param id task ID from the request path
     * @param task merged task data
     * @param dependencyIds replacement dependency IDs, or {@code null} to keep existing dependencies
     * @return managed updated task
     */
    @Transactional
    public DynamicTask updateDynamicTask(Long id, DynamicTask task, List<Long> dependencyIds) {
        return updateDynamicTask(id, task, dependencyIds, null);
    }

    /**
     * Updates a dynamic task, optionally changing organization and dependency edges.
     *
     * @param id task ID from the request path
     * @param task merged task data
     * @param dependencyIds replacement dependency IDs, or {@code null} to keep existing dependencies
     * @param organizationId replacement organization ID, or {@code null} to keep the current organization
     * @return managed updated task
     */
    @Transactional
    public DynamicTask updateDynamicTask(Long id, DynamicTask task, List<Long> dependencyIds, String organizationId) {
        if (!Objects.equals(id, task.getId())) {
            throw new InvalidRequestException("Task id in path does not match target task");
        }

        Task persistedTask = this.taskRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));
        if (!(persistedTask instanceof DynamicTask managedTask)) {
            throw new InvalidRequestException("Task type mismatch: expected dynamic task");
        }

        if (task.getAccount() != null) {
            managedTask.setAccount(task.getAccount());
        }
        validateAccountOrgAccess(managedTask, organizationId);
        managedTask.setName(task.getName());
        managedTask.setDescription(task.getDescription());
        managedTask.setDifficulty(task.getDifficulty());
        managedTask.setStartAt(task.getStartAt());
        managedTask.setEndAt(task.getEndAt());
        managedTask.setDuration(task.getDuration());
        managedTask.setElapsed(task.getElapsed());
        managedTask.setMinScopeDuration(task.getMinScopeDuration());
        managedTask.setMaxScopeDuration(task.getMaxScopeDuration());
        managedTask.setOrganization(organizationId);

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

    /**
     * Updates a fixed-time task without changing organization.
     *
     * @param id task ID from the request path
     * @param task merged task data
     * @return managed updated task
     */
    @Transactional
    public StaticTask updateStaticTask(Long id, StaticTask task) {
        return updateStaticTask(id, task, null);
    }

    /**
     * Updates a fixed-time task and optionally changes its organization.
     *
     * @param id task ID from the request path
     * @param task merged task data
     * @param organizationId replacement organization ID, or {@code null} to keep the current organization
     * @return managed updated task
     */
    @Transactional
    public StaticTask updateStaticTask(Long id, StaticTask task, String organizationId) {
        if (!Objects.equals(id, task.getId())) {
            throw new InvalidRequestException("Task id in path does not match target task");
        }

        Task persistedTask = this.taskRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));
        if (!(persistedTask instanceof StaticTask managedTask)) {
            throw new InvalidRequestException("Task type mismatch: expected static task");
        }

        if (task.getAccount() != null) {
            managedTask.setAccount(task.getAccount());
        }
        validateAccountOrgAccess(managedTask, organizationId);
        managedTask.setName(task.getName());
        managedTask.setDescription(task.getDescription());
        managedTask.setDifficulty(task.getDifficulty());
        managedTask.setStartAt(task.getStartAt());
        managedTask.setEndAt(task.getEndAt());
        managedTask.setIsBlocker(task.getIsBlocker());
        managedTask.setOrganization(organizationId);

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

    /**
     * Verifies that the account is linked to the requested organization.
     *
     * @param task task to validate
     * @param organizationId organization that must be accessible
     */
    public void validateAccountOrgAccess(Task task, String organizationId) {
        List<OrganizationRepresentation> organizations = this.keycloakService.getAccountOrganizations(task.getAccount().getSubject());
        if (organizations.stream().noneMatch(o -> Objects.equals(o.getId(), organizationId))) {
            throw new AccountAccessDeniedException("Account does not have access to the specified organization");
        }
    }

    private void validateStaticTask(StaticTask task) {
        validateCommonTaskFields(task);
        if (task.getIsBlocker() == null) {
            throw new InvalidRequestException("isBlocker must be provided");
        }
        if (!Boolean.TRUE.equals(task.getIsBlocker()) && task.getOrganization() == null) {
            throw new InvalidRequestException("organizationId is required unless isBlocker is true");
        }
    }

    private void validateAndNormalizeDynamicTask(DynamicTask task) {
        validateCommonTaskFields(task);
        if (task.getOrganization() == null) {
            throw new InvalidRequestException("organizationId must be provided");
        }

        Duration duration = task.getDuration();
        Duration minScopeDuration = task.getMinScopeDuration();
        Duration maxScopeDuration = task.getMaxScopeDuration();

        if (duration == null) {
            throw new InvalidRequestException("duration must be provided");
        }
        if (duration.isZero() || duration.isNegative()) {
            throw new InvalidRequestException("duration must be greater than 0");
        }
        if (minScopeDuration.isZero() || minScopeDuration.isNegative()) {  
            throw new InvalidRequestException("minScopeDuration must be greater than 0");  
        }  
        if (maxScopeDuration.isZero() || maxScopeDuration.isNegative()) {  
            throw new InvalidRequestException("maxScopeDuration must be greater than 0");  
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
        Set<Long> dependencyIdSet = new HashSet<>(dependencyIds);

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

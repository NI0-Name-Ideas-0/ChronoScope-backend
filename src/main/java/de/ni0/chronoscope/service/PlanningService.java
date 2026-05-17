package de.ni0.chronoscope.service;

import de.ni0.chronoscope.algorithm.Algorithm;
import de.ni0.chronoscope.algorithm.BlockedInterval;
import de.ni0.chronoscope.algorithm.ConcreteWorkSlot;
import de.ni0.chronoscope.algorithm.StaticTaskExpander;
import de.ni0.chronoscope.algorithm.TaskGraphNode;
import de.ni0.chronoscope.algorithm.WeightDataProvider;
import de.ni0.chronoscope.algorithm.WorkSlotCarver;
import de.ni0.chronoscope.algorithm.WorkSlotExpander;
import de.ni0.chronoscope.algorithm.WorkSlotProvider;
import de.ni0.chronoscope.algorithm.dataprovider.CPMDataProvider;
import de.ni0.chronoscope.algorithm.dataprovider.DifficultyDataProvider;
import de.ni0.chronoscope.exception.InsufficientSlotsException;
import de.ni0.chronoscope.exception.InvalidRequestException;
import de.ni0.chronoscope.model.*;
import de.ni0.chronoscope.repository.ScopeRepository;
import de.ni0.chronoscope.repository.TaskRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Application service that turns dynamic tasks and work slots into persisted planned scopes.
 */
@Service
@RequiredArgsConstructor
public class PlanningService {

    private final TaskRepository taskRepository;
    private final ScopeRepository scopeRepository;
    private final WorkSlotService workSlotService;
    private final KeycloakService keycloakService;
    private final WorkSlotExpander workSlotExpander;
    private final StaticTaskExpander staticTaskExpander;
    private final WorkSlotCarver workSlotCarver;

    /**
     * Replans all dynamic tasks for an account and organizationId.
     *
     * <p>Existing scopes for the planned tasks are deleted only after a valid replacement plan
     * has been calculated.</p>
     *
     * @param identity identity whose tasks should be planned
     * @param orgId organizationId to constrain the plan to
     * @return newly persisted scopes, or an empty list when there is nothing to plan
     */
    @Transactional
    public List<Scope> planTasksForIdentity(Identity identity, String orgId) {
        keycloakService.validateIdentityOrgAccess(identity, orgId);
        var dynamicTasks = taskRepository.findDynamicTasksByIdentityIdAndOrganizationId(identity.getId(), orgId);

        if (dynamicTasks.isEmpty()) {
            // An exception would imply something went wrong, but "nothing to plan" is not a failure.
            // therefore we return an empty plan instead of throwing an exception in this case.
            return List.of();
        }
        List<Scope> activeScopes = scopeRepository.findActiveScope(identity.getId());
        if (activeScopes.size() > 1) {
            throw new IllegalStateException(
                    "More than one active scope found for identityId=" + identity.getId() + ", orgId=" + orgId
                            + " (activeScopes=" + activeScopes.size() + ")");
        }
        Scope activeScope = activeScopes.isEmpty() ? null : activeScopes.getFirst();

        var recurringSlots = workSlotService.getWorkSlotsForIdentity(identity.getId(), orgId);
        var staticTasks = taskRepository.findStaticTasksByIdentityId(identity.getId());

        var planningResult = plan(dynamicTasks, activeScope, recurringSlots, staticTasks);

        if (planningResult == null) {
            throw new InsufficientSlotsException("No valid plan could be found with the available work slots");
        }

        Set<Scope> futureScopes = new HashSet<>(scopeRepository.getScopesByDynamicTaskOrganizationIdAndDynamicTaskIdentityId(orgId, identity.getId()));
        if (activeScope != null) {
            futureScopes.remove(activeScope);
        }
        futureScopes.removeIf(s -> s.getEndAt().isBefore(Instant.now()));
        scopeRepository.deleteAllInBatch(futureScopes); // Clear old scopes
        scopeRepository.saveAll(planningResult); // Save new scopes

        return planningResult;
    }

    private List<Scope> plan(List<DynamicTask> tasks, Scope activeScope, List<WorkSlot> recurringSlots, List<StaticTask> staticTasks) {
        if (recurringSlots == null || recurringSlots.isEmpty()) {
            throw new InsufficientSlotsException("No work slots are available for planning");
        }

        // Determine planning horizon: furthest task deadline, extended by one week as buffer
        Instant now = Instant.now();
        if (activeScope != null) {
            now = activeScope.getEndAt();
        }
        Instant horizon = tasks.stream()
                .map(Task::getEndAt)
                .max(Comparator.naturalOrder())
                .orElse(now)
                .plusSeconds(7L * 24 * 60 * 60);

        List<ConcreteWorkSlot> slots = workSlotExpander.expand(recurringSlots, now, horizon);
        if (slots.isEmpty()) {
            throw new InsufficientSlotsException("No work slots fall within the planning horizon");
        }

        // Carve out time blocks occupied by static-task occurrences.
        if (!staticTasks.isEmpty()) {
            List<BlockedInterval> blocked = staticTaskExpander.expand(staticTasks, now, horizon);
            slots = workSlotCarver.carve(slots, blocked);
            if (slots.isEmpty()) {
                throw new InsufficientSlotsException("No work slots remain after blocking static task time");
            }
        }

        List<TaskGraphNode> taskNodes = toTaskGraphNodes(tasks);
        Map<TaskGraphNode, Integer> uncompletedDependencies = new HashMap<>();
        Map<TaskGraphNode, Duration> remainingTaskDurationMap = new HashMap<>();
        List<TaskGraphNode> startNodes = new ArrayList<>();

        for (TaskGraphNode node : taskNodes) {
            int dependencyCount = getUncompletedDependencies(node, activeScope).size();
            uncompletedDependencies.put(node, dependencyCount);
            Duration remainingTaskDuration = node.getRemaining();

            // If there is an active scope we want  to respect it when planning
            if (activeScope != null && activeScope.getDynamicTask().getId().equals(node.task().getId())) {
                if (activeScope.getEndAt().isBefore(activeScope.getStartAt())) {
                    throw new InvalidRequestException("Active scope end time must not be before start time");
                }
                Duration activeScopeDuration = Duration.between(activeScope.getStartAt(), activeScope.getEndAt());

                List<Scope> scopes = activeScope.getDynamicTask().getScopes();
                Duration scopeDurationSum = Duration.ZERO;
                for (Scope scope : scopes) {
                    if (scope.getId().equals(activeScope.getId())) break;
                    scopeDurationSum = scopeDurationSum.plus(Duration.between(scope.getStartAt(), scope.getEndAt()));
                }
                // We only subtract the current scope duration if it is not marked as done yet.
                // Consider Scopes: 15-15-15-15
                // If the user works on scope 1, marks it as complete while working on it,
                // the elapsed time would be increased to 15, but because the algorithm thinks the
                // current scope needs to be subtracted the algorithm would only plan for 30 minutes
                if (activeScope.getDynamicTask().getElapsed().compareTo(scopeDurationSum) <= 0) {
                    remainingTaskDuration = remainingTaskDuration.minus(activeScopeDuration);
                }
            }

            if (!remainingTaskDuration.isPositive()) {
                continue;
            }

            remainingTaskDurationMap.put(node, remainingTaskDuration);
            if (dependencyCount == 0) {
                startNodes.add(node);
            }
        }

        if (startNodes.isEmpty()) {
            throw new InvalidRequestException("Did not find any nodes to start");
        }

        List<WeightDataProvider> providers = List.of(
                new CPMDataProvider(),
                new DifficultyDataProvider()
        );
        Algorithm algorithm = new Algorithm(providers);
        WorkSlotProvider workSlotProvider = new WorkSlotProvider(slots);
        ConcreteWorkSlot startSlot = workSlotProvider.getNextSlot(null);

        return algorithm.plan(startNodes, uncompletedDependencies,
                remainingTaskDurationMap, workSlotProvider, startSlot,
                startSlot.startAt(),
                new ArrayList<>());
    }

    private List<TaskGraphNode> getUncompletedDependencies(TaskGraphNode node, Scope activeScope) {
        List<TaskGraphNode> uncompletedDependencies = node.getUncompletedDependencies();
        return uncompletedDependencies.stream().filter(d -> {
            if (activeScope != null && d.task().getId().equals(activeScope.getDynamicTask().getId())) {
                return d.getRemaining().minus(Duration.between(activeScope.getStartAt(), activeScope.getEndAt()))
                        .isPositive();
            }
            return true;
        }).toList();
    }

    private List<TaskGraphNode> toTaskGraphNodes(List<DynamicTask> tasks) {
        Map<DynamicTask, TaskGraphNode> nodesByTask = new HashMap<>();

        for (DynamicTask task : tasks) {
            nodesByTask.put(task, new TaskGraphNode(task, new ArrayList<>(), new ArrayList<>()));
        }

        for (DynamicTask task : tasks) {
            TaskGraphNode node = nodesByTask.get(task);

            for (DynamicTask dependency : task.getDependencies()) {
                TaskGraphNode dependencyNode = nodesByTask.get(dependency);
                if (dependencyNode == null) {
                    throw new InvalidRequestException(
                            "Task " + task.getId() + " has a dependency (id=" + dependency.getId() + ") outside the planned organizationId scope");
                }
                node.dependencies().add(dependencyNode);
            }

            for (DynamicTask dependent : task.getDependents()) {
                TaskGraphNode dependentNode = nodesByTask.get(dependent);
                if (dependentNode == null) {
                    throw new InvalidRequestException(
                            "Task " + task.getId() + " has a dependent (id=" + dependent.getId() + ") outside the planned organizationId scope");
                }
                node.dependents().add(dependentNode);
            }
        }

        return new ArrayList<>(nodesByTask.values());
    }
}

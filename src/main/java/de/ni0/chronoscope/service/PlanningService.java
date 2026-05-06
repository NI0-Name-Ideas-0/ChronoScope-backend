package de.ni0.chronoscope.service;

import de.ni0.chronoscope.algorithm.Algorithm;
import de.ni0.chronoscope.algorithm.TaskGraphNode;
import de.ni0.chronoscope.algorithm.WeightDataProvider;
import de.ni0.chronoscope.algorithm.WorkSlotProvider;
import de.ni0.chronoscope.algorithm.dataprovider.CPMDataProvider;
import de.ni0.chronoscope.exception.InsufficientSlotsException;
import de.ni0.chronoscope.exception.InvalidRequestException;
import de.ni0.chronoscope.model.*;
import de.ni0.chronoscope.repository.ScopeRepository;
import de.ni0.chronoscope.repository.TaskRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Application service that turns dynamic tasks and work slots into persisted planned scopes.
 */
@Service
@RequiredArgsConstructor
public class PlanningService {

    private final TaskRepository taskRepository;
    private final ScopeRepository scopeRepository;
    private final WorkSlotService workSlotService;
    private final AccountService accountService;
    private final KeycloakService keycloakService;

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

        var dynamicTaskIds = dynamicTasks.stream().map(DynamicTask::getId).toList();
        var workSlots = workSlotService.getWorkSlotsForIdentity(identity.getId(), orgId);

        var planningResult = plan(dynamicTasks, workSlots);

        if (planningResult == null) {
            throw new InsufficientSlotsException("No valid plan could be found with the available work slots");
        }

        scopeRepository.deleteByDynamicTaskIdIn(dynamicTaskIds); // Clear old scopes
        scopeRepository.saveAll(planningResult); // Save new scopes

        return planningResult;
    }

    private List<Scope> plan(List<DynamicTask> tasks, List<WorkSlot> slots) {
        if (slots == null || slots.isEmpty()) {
            throw new InsufficientSlotsException("No work slots are available for planning");
        }

        List<TaskGraphNode> taskNodes = toTaskGraphNodes(tasks);
        Map<TaskGraphNode, Integer> dependencyCountMap = new HashMap<>();
        Map<TaskGraphNode, Duration> remainingTaskDurationMap = new HashMap<>();
        List<TaskGraphNode> startNodes = new ArrayList<>();

        for (TaskGraphNode node : taskNodes) {
            int dependencyCount = node.dependencies().size();
            dependencyCountMap.put(node, dependencyCount);
            remainingTaskDurationMap.put(node, node.getDuration());
            if (dependencyCount == 0) {
                startNodes.add(node);
            }
        }

        if (startNodes.isEmpty()) {
            throw new InvalidRequestException("Tasks contain a dependency cycle: no task has zero dependencies");
        }

        List<WeightDataProvider> providers = List.of(new CPMDataProvider());
        Algorithm algorithm = new Algorithm(providers);
        WorkSlotProvider workSlotProvider = new WorkSlotProvider(slots);
        WorkSlot startSlot = workSlotProvider.getNextSlot(null);

        return algorithm.plan(startNodes, dependencyCountMap,
                remainingTaskDurationMap, workSlotProvider, startSlot,
                startSlot.getStartAt());
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

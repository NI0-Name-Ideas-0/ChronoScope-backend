package de.ni0.chronoscope.service;

import de.ni0.chronoscope.algorithm.Algorithm;
import de.ni0.chronoscope.algorithm.TaskGraphNode;
import de.ni0.chronoscope.algorithm.WeightDataProvider;
import de.ni0.chronoscope.algorithm.WorkSlotProvider;
import de.ni0.chronoscope.algorithm.dataprovider.CPMDataProvider;
import de.ni0.chronoscope.model.DynamicTask;
import de.ni0.chronoscope.model.Scope;
import de.ni0.chronoscope.model.WorkSlot;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlanningService {

    public List<Scope> plan(List<DynamicTask> tasks, List<WorkSlot> slots) {
        if (slots == null || slots.isEmpty()) {
            throw new IllegalArgumentException("Slots must not be null or empty");
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
                    throw new IllegalArgumentException(
                            "Planning relation points to a task outside the planned task set: " + dependency.getId());
                }
                node.dependencies().add(dependencyNode);
            }

            for (DynamicTask dependent : task.getDependents()) {
                TaskGraphNode dependentNode = nodesByTask.get(dependent);
                if (dependentNode == null) {
                    throw new IllegalArgumentException(
                            "Planning relation points to a task outside the planned task set: " + dependent.getId());
                }
                node.dependents().add(dependentNode);
            }
        }

        return new ArrayList<>(nodesByTask.values());
    }
}

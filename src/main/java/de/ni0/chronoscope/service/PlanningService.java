package de.ni0.chronoscope.service;

import de.ni0.chronoscope.algorithm.Algorithm;
import de.ni0.chronoscope.algorithm.Task;
import de.ni0.chronoscope.algorithm.WeightDataProvider;
import de.ni0.chronoscope.algorithm.WorkSlotProvider;
import de.ni0.chronoscope.algorithm.dataprovider.CPMDataProvider;
import de.ni0.chronoscope.model.DynamicTask;
import de.ni0.chronoscope.model.Scope;
import de.ni0.chronoscope.model.TaskDependency;
import de.ni0.chronoscope.model.WorkSlot;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlanningService {

    public List<Scope> plan(List<DynamicTask> tasks,
                            List<WorkSlot> slots) {
        Map<DynamicTask, Task> taskMap = new HashMap<>();
        for (DynamicTask task : tasks) {
            taskMap.put(task, new Task(task, new ArrayList<>(), new ArrayList<>()));
        }
        for (DynamicTask task : tasks) {
            for (TaskDependency dependency : task.getDependencies()) {
                Task algTask = taskMap.get(task);
                Task algDependency = taskMap.get(dependency.getPredecessor());
                algTask.dependencies().add(algDependency);
                algDependency.successors().add(algTask);
            }
        }
        List<Task> algTasks = new ArrayList<>(taskMap.values());

        return this.plan2(algTasks, slots);
    }

    private List<Scope> plan2(List<Task> tasks, List<WorkSlot> slots) {
        Map<Task, Integer> dependencyCount = new HashMap<>();
        Map<Task, Duration> remainingTaskDurations = new HashMap<>();
        for (Task task : tasks) {
            dependencyCount.put(task, task.dependencies().size());
            remainingTaskDurations.put(task, task.duration());
        }
        List<Task> startTasks = new ArrayList<>();
        dependencyCount.forEach((k, v) -> {
            if (v == 0) {
                startTasks.add(k);
            }
        });
        if (slots == null || slots.isEmpty()) {
            throw new IllegalArgumentException("slots must not be null or empty");
        }
        List<WeightDataProvider> providers = List.of(
                new CPMDataProvider()
        );
        Algorithm algorithm = new Algorithm(providers);
        WorkSlotProvider workSlotProvider = new WorkSlotProvider(slots);
        WorkSlot startSlot = workSlotProvider.getNextSlot(null);
        return algorithm.plan(startTasks, dependencyCount,
                remainingTaskDurations, workSlotProvider, startSlot,
                startSlot.getStartAt());
    }

}

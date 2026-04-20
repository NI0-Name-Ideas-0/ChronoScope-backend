package de.ni0.chronoscope.algorithm;

import de.ni0.chronoscope.algorithm.dataprovider.CPMDataProvider;
import de.ni0.chronoscope.model.DynamicTask;
import de.ni0.chronoscope.model.Scope;
import de.ni0.chronoscope.model.TaskDependency;
import de.ni0.chronoscope.model.WorkSlot;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Test {

    static void main() {
        List<DynamicTask> tasks = new ArrayList<>();
        DynamicTask task = new DynamicTask("test", 0,
                Instant.MIN,
                Instant.MIN.plus(5, ChronoUnit.HOURS),
                Duration.of(1, ChronoUnit.HOURS),
                Duration.of(0, ChronoUnit.HOURS),
                Duration.of(1, ChronoUnit.HOURS),
                Duration.of(10, ChronoUnit.HOURS));
        task.setId(0L);
        DynamicTask task2 = new DynamicTask("test2", 0,
                Instant.MIN,
                Instant.MIN.plus(9, ChronoUnit.HOURS),
                Duration.of(8, ChronoUnit.HOURS),
                Duration.of(0, ChronoUnit.HOURS),
                Duration.of(1, ChronoUnit.HOURS),
                Duration.of(10, ChronoUnit.HOURS));
        task2.setId(1L);
        tasks.add(task);
        tasks.add(task2);
        List<WorkSlot> slots = new ArrayList<>();
        WorkSlot slot = new WorkSlot(
                Instant.MIN, Instant.MIN.plus(Duration.of(2, ChronoUnit.HOURS))
        );
        WorkSlot slot2 = new WorkSlot(
                Instant.MIN.plus(Duration.of(2, ChronoUnit.HOURS)), Instant.MIN.plus(Duration.of(9, ChronoUnit.HOURS))
        );
        slots.add(slot);
        slots.add(slot2);

        Test test = new Test();
        List<Scope> scopes = test.plan(tasks, slots);
            if (scopes == null) {
            throw new IllegalStateException("Did not find result");
        }
        for (Scope scope : scopes) {
            System.out.println("Scope for " + scope.getDynamicTask().getId() + " from " + scope.getStartAt() + " -> " + scope.getEndAt());
        }
    }

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

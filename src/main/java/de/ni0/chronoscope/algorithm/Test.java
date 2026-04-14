package de.ni0.chronoscope.algorithm;

import de.ni0.chronoscope.algorithm.dataprovider.CPMDataProvider;
import de.ni0.chronoscope.model.DynamicTask;
import de.ni0.chronoscope.model.OrganizationSlot;
import de.ni0.chronoscope.model.Scope;

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
        DynamicTask task = new DynamicTask("test", "test",
                Instant.MIN,
                Instant.MIN.plus(5, ChronoUnit.HOURS),
                Duration.of(1, ChronoUnit.HOURS),
                0);
        task.setId(0L);
        DynamicTask task2 = new DynamicTask("test2", "test",
                Instant.MIN,
                Instant.MIN.plus(9, ChronoUnit.HOURS),
                Duration.of(8, ChronoUnit.HOURS),
                0);
        task2.setId(1L);
        tasks.add(task);
        tasks.add(task2);
        List<OrganizationSlot> slots = new ArrayList<>();
        OrganizationSlot slot = new OrganizationSlot(
                Instant.MIN, Duration.of(2, ChronoUnit.HOURS)
        );
        OrganizationSlot slot2 = new OrganizationSlot(
                Instant.MIN.plus(Duration.of(2, ChronoUnit.HOURS)), Duration.of(7, ChronoUnit.HOURS)
        );
        slots.add(slot);
        slots.add(slot2);

        Test test = new Test();
        List<Scope> scopes = test.plan(tasks, slots);
            if (scopes == null) {
            throw new IllegalStateException("Did not find result");
        }
            for (Scope scope : scopes) {
            System.out.println("Scope for " + scope.getTask().getId() + " from " + scope.getStart() + " -> " + scope.getDuration());
        }
    }

    public List<Scope> plan(List<DynamicTask> tasks,
                            List<OrganizationSlot> slots) {
        Map<DynamicTask, Task> taskMap = new HashMap<>();
        for (DynamicTask task : tasks) {
            taskMap.put(task, new Task(task, new ArrayList<>(), new ArrayList<>()));
        }
        for (DynamicTask task : tasks) {
            for (DynamicTask dependency : task.getDependencies()) {
                Task algTask = taskMap.get(task);
                Task algDependency = taskMap.get(dependency);
                algTask.dependencies().add(algDependency);
                algDependency.successors().add(algTask);
            }
        }
        List<Task> algTasks = new ArrayList<>(taskMap.values());

        return this.plan2(algTasks, slots);
    }

    private List<Scope> plan2(List<Task> tasks, List<OrganizationSlot> slots) {
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
        return algorithm.plan(startTasks, dependencyCount,
                remainingTaskDurations, slots, 0,
                Duration.ZERO);
    }

}

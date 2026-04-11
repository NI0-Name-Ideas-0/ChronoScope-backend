package de.ni0.chronoscope.algorithm;

import de.ni0.chronoscope.model.OrganizationSlot;
import de.ni0.chronoscope.model.Scope;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class Algorithm {

    static void main() {
        Algorithm algorithm = new Algorithm();
        List<Task> tasks = new ArrayList<>();
        Task task = new Task(0,0,
                Duration.of(1, ChronoUnit.HOURS),
                Instant.MIN,
                Instant.MIN.plus(5, ChronoUnit.HOURS),
                new ArrayList<>(),
                new ArrayList<>());
        Task task2 = new Task(1, 0,
                Duration.of(8, ChronoUnit.HOURS),
                Instant.now(),
                Instant.now().plus(9, ChronoUnit.HOURS),
                new ArrayList<>(),
                new ArrayList<>());
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


        List<Scope> scopes = algorithm.plan(tasks, slots);
        if (scopes == null) {
            throw new IllegalStateException("Did not find result");
        }
        for (Scope scope : scopes) {
            System.out.println("Scope for " + scope.getTask().getId() + " from " + scope.getStart() + " -> " + scope.getDuration());
        }
    }

    public List<Scope> plan(List<Task> tasks, List<OrganizationSlot> slots) {
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
        return this.plan(0, startTasks, dependencyCount,
                remainingTaskDurations, slots, 0,
                Duration.ZERO);
    }

    public List<Scope> plan(int depth, List<Task> tasks,
                            Map<Task, Integer> dependencyCount,
                            Map<Task, Duration> remainingTaskDurations,
                            List<OrganizationSlot> slots, int slotIndex,
                            Duration elapsedSlotTime) {
        System.out.println("----- " + depth + " -----");
        OrganizationSlot slot = slots.get(slotIndex);
        Instant currentTime = slot.getStart().plus(elapsedSlotTime);
        System.out.println("Planning tasks " + tasks + " in slot " + slotIndex);
        Duration remainingSlotDuration = slot.getDuration().minus(elapsedSlotTime);
        System.out.println(remainingSlotDuration + " time left in slot");

        CPM cpm = new CPM(tasks, currentTime);
        tasks.sort((t1, t2) -> Double.compare(getWeight(cpm, t1), getWeight(cpm, t2)));

        for (Task task : new ArrayList<>(tasks)) {
            int newSlotIndex = slotIndex;
            System.out.println("Task Depth: ----- " + depth + " ----- Slot " + newSlotIndex);

            System.out.println("Chose Task: " + task);

            List<Scope> scopes = new ArrayList<>();

            Duration remainingTaskDuration = remainingTaskDurations.get(task);
            Duration scopeDuration = remainingTaskDuration.compareTo(remainingSlotDuration) <= 0
                    ? remainingTaskDuration
                    : remainingSlotDuration;
            System.out.println("Scope Duration: " + scopeDuration);
            if (currentTime.plus(scopeDuration).isAfter(task.end())) {
                System.out.println("Deadline not met");
                continue;
            }
            Duration newRemainingTaskDuration = remainingTaskDurations.get(task).minus(scopeDuration);
            remainingTaskDurations.put(task, newRemainingTaskDuration);

            // Only calculate new possible tasks if current one has been completly planned
            if (newRemainingTaskDuration.isZero()) {
                tasks.remove(task);
                for (Task successor : task.successors()) {
                    int newCount = dependencyCount.get(successor) - 1;
                    dependencyCount.put(successor, newCount);
                    if (newCount == 0) {
                        tasks.add(successor);
                    }
                }
                System.out.println("Updated Tasks: " + tasks);
            }

            Duration newElapsedSlotTime = elapsedSlotTime.plus(scopeDuration);
            System.out.println("Elapsed slot time: " + newElapsedSlotTime);
            if (newElapsedSlotTime.equals(slot.getDuration())) {
                System.out.println("Next slot: " + newSlotIndex);
                newSlotIndex++;
                newElapsedSlotTime = Duration.ZERO;
            }

            scopes.add(new Scope(new de.ni0.chronoscope.model.Task(), currentTime, scopeDuration));

            if (!tasks.isEmpty()) {
                List<Scope> nextResult = plan(depth + 1, tasks, dependencyCount, remainingTaskDurations,
                        slots, newSlotIndex, newElapsedSlotTime);
                if (nextResult != null) {
                    scopes.addAll(nextResult);
                    return scopes;
                }
                System.out.println("Path did not return result");
            } else {
                return scopes;
            }

            // Reset for backtracking
            remainingTaskDurations.put(task, remainingTaskDurations.get(task).plus(scopeDuration));
            if (newRemainingTaskDuration.isZero()) {
                tasks.add(task);
                for (Task successor : task.successors()) {
                    int newCount = dependencyCount.get(successor) + 1;
                    dependencyCount.put(successor, newCount);
                    if (newCount == 0) {
                        tasks.remove(successor);
                    }
                }
            }
        }
        return null;
    }

    public double getWeight(CPM cpm, Task task) {
        return 1.0d/ cpm.getTaskData().get(task).getSlack().toMinutes();
    }

}

package de.ni0.chronoscope.algorithm;

import de.ni0.chronoscope.model.OrganizationSlot;
import de.ni0.chronoscope.model.Scope;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@RequiredArgsConstructor
public class Algorithm {

    private final List<WeightDataProvider> providers;

    public List<Scope> plan(List<Task> tasks,
                            Map<Task, Integer> dependencyCount,
                            Map<Task, Duration> remainingTaskDurations,
                            List<OrganizationSlot> slots, int slotIndex,
                            Duration elapsedSlotTime) {
        OrganizationSlot slot = slots.get(slotIndex);
        Instant currentTime = slot.getStart().plus(elapsedSlotTime);
        System.out.println("Planning tasks " + tasks + " in slot " + slotIndex);
        Duration remainingSlotDuration = slot.getDuration().minus(elapsedSlotTime);
        System.out.println(remainingSlotDuration + " time left in slot");

        for (Task task : tasks) {
            if (currentTime.plus(remainingTaskDurations.get(task)).isAfter(task.end())) {
                System.out.println("Deadline not met");
                return null;
            }
        }

        for (WeightDataProvider provider : this.providers) {
            provider.calculate(new DataProviderContext(currentTime), tasks);
        }
        tasks.sort((t1, t2) -> -1*Double.compare(getWeight(t1), getWeight(t2)));

        for (Task task : new ArrayList<>(tasks)) {
            int newSlotIndex = slotIndex;

            System.out.println("Chose Task: " + task);

            List<Scope> scopes = new ArrayList<>();

            Duration remainingTaskDuration = remainingTaskDurations.get(task);
            Duration scopeDuration = remainingTaskDuration.compareTo(remainingSlotDuration) <= 0
                    ? remainingTaskDuration
                    : remainingSlotDuration;
            System.out.println("Scope Duration: " + scopeDuration);
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
                List<Scope> nextResult = plan(tasks, dependencyCount, remainingTaskDurations,
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

    private double getWeight(Task task) {
        double weight = 0;
        for (WeightDataProvider provider : this.providers) {
            weight += provider.getWeight(task);
        }
        return weight;
    }

}

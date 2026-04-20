package de.ni0.chronoscope.algorithm;

import de.ni0.chronoscope.model.Scope;
import de.ni0.chronoscope.model.WorkSlot;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@RequiredArgsConstructor
public class Algorithm {

    private final List<WeightDataProvider> providers;

    /**
     * @param tasks All next tasks
     * @param dependencyCount Dependency Count
     * @param remainingTaskDurations All the remaining task durations. Only contains non-completed tasks
     * @param slots Iterator which always returns the next slot to implement
     * @param slot The current processed slot
     * @param currentTime The current time
     * @return The planned scopes in the current path after this node
     */
    public List<Scope> plan(List<Task> tasks,
                            Map<Task, Integer> dependencyCount,
                            Map<Task, Duration> remainingTaskDurations,
                            WorkSlotProvider slots,
                            WorkSlot slot,
                            Instant currentTime) {
        System.out.println("Planning tasks " + tasks + " in slot");
        Duration remainingSlotDuration = currentTime.until(slot.getEndAt());
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
            System.out.println("Chose Task: " + task);

            List<Scope> scopes = new ArrayList<>();

            Duration remainingTaskDuration = remainingTaskDurations.get(task);
            System.out.println("Remaining Task Duration: " + remainingTaskDuration);
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

            Instant newCurrentTime = currentTime.plus(scopeDuration);
            WorkSlot newWorkSlot = slot;
            if (newCurrentTime.equals(slot.getEndAt())) {
                System.out.println("Using next slot");
                newWorkSlot = slots.getNextSlot(slot);
                newCurrentTime = newWorkSlot.getStartAt();
            }

            scopes.add(new Scope(null, task.task(), currentTime, currentTime.plus(scopeDuration)));

            if (!tasks.isEmpty()) {
                List<Scope> nextResult = plan(tasks, dependencyCount, remainingTaskDurations,
                        slots, newWorkSlot, newCurrentTime);
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
        System.out.println("Path did not found result");
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

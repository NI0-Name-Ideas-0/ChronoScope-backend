package de.ni0.chronoscope.algorithm;

import de.ni0.chronoscope.model.Scope;
import de.ni0.chronoscope.model.WorkSlot;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Backtracking scheduler that assigns ready dynamic tasks into available work slots.
 *
 * <p>The algorithm repeatedly weighs all dependency-ready tasks, places the most promising
 * task into the current slot, and backtracks when a branch misses a deadline or cannot be
 * completed from the current state.</p>
 */
@RequiredArgsConstructor
public class Algorithm {

    private static final Logger log = LoggerFactory.getLogger(Algorithm.class);

    private final List<WeightDataProvider> providers;

    /**
     * Plans scopes for the currently ready tasks from the given point in time.
     *
     * <p>The input collections are mutated while exploring a branch and restored when the
     * branch fails, so callers should pass planner-owned state rather than shared state.</p>
     *
     * @param tasks dependency-ready tasks that can be scheduled next
     * @param dependencyCount remaining unresolved dependency count per task
     * @param remainingTaskDurations remaining unscheduled duration for each incomplete task
     * @param slots provider used to move to the next available work slot
     * @param slot current work slot
     * @param currentTime current cursor inside {@code slot}
     * @return planned scopes for a successful branch, or {@code null} when no valid plan exists
     */
    public List<Scope> plan(List<TaskGraphNode> tasks,
                            Map<TaskGraphNode, Integer> dependencyCount,
                            Map<TaskGraphNode, Duration> remainingTaskDurations,
                            WorkSlotProvider slots,
                            WorkSlot slot,
                            Instant currentTime) {
        log.debug("Planning tasks {} in slot", tasks);
        Duration remainingSlotDuration = currentTime.until(slot.getEndAt());
        log.debug("{} time left in slot", remainingSlotDuration);

        for (TaskGraphNode task : tasks) {
            if (currentTime.plus(remainingTaskDurations.get(task)).isAfter(task.getEndAt())) {
                log.debug("Deadline not met");
                return null;
            }
        }

        for (WeightDataProvider provider : this.providers) {
            provider.calculate(new DataProviderContext(currentTime), tasks);
        }
        tasks.sort((t1, t2) -> -1*Double.compare(getWeight(t1), getWeight(t2)));

        for (TaskGraphNode task : new ArrayList<>(tasks)) {
            log.debug("Chose Task: {}", task);

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
                for (TaskGraphNode successor : task.dependents()) {
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
                if (newWorkSlot == null) {
                    return null;
                }
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
                for (TaskGraphNode successor : task.dependents()) {
                    int oldCount = dependencyCount.get(successor);
                    int newCount = oldCount + 1;
                    dependencyCount.put(successor, newCount);
                    if (oldCount == 0) {
                        tasks.remove(successor);
                    }
                }
            }
        }
        log.debug("Path did not found result");
        return null;
    }

    private double getWeight(TaskGraphNode task) {
        double weight = 0;
        for (WeightDataProvider provider : this.providers) {
            weight += provider.getWeight(task);
        }
        return weight;
    }

}

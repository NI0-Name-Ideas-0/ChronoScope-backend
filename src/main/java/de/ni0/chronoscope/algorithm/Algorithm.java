package de.ni0.chronoscope.algorithm;

import de.ni0.chronoscope.model.Scope;
import de.ni0.chronoscope.model.WorkSlot;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@RequiredArgsConstructor
public class Algorithm {

    private static final Logger log = LoggerFactory.getLogger(Algorithm.class);

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
                    int newCount = dependencyCount.get(successor) + 1;
                    dependencyCount.put(successor, newCount);
                    if (newCount == 0) {
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

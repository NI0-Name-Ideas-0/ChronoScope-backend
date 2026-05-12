package de.ni0.chronoscope.algorithm;

import de.ni0.chronoscope.model.Scope;
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
                            ConcreteWorkSlot slot,
                            Instant currentTime,
                            List<Scope> plannedScopes) {
        log.debug("Planning tasks {} in slot", tasks);
        Duration remainingSlotDuration = currentTime.until(slot.endAt());
        log.debug("{} time left in slot", remainingSlotDuration);

        if (tasks.isEmpty()) {
            return List.of();
        }
        boolean hasRemainingWork = tasks.stream()
                .anyMatch(task -> remainingTaskDurations.get(task).isPositive());
        if (!hasRemainingWork) {
            return List.of();
        }

        CPM cpm = new CPM(tasks, currentTime);
        for (TaskGraphNode task : tasks) {
            if (cpm.getTaskData().get(task).getSlack().isNegative()) {
                log.debug("Deadline not met");
                return null;
            }
        }

        // Advance to the next slot if no pending task can start: either its minimum scope does not fit
        // the remaining time or its startAt has not been reached yet.
        List<TaskGraphNode> possibleTasks = new ArrayList<>(
                tasks.stream().filter(
                t -> remainingSlotDuration.compareTo(t.getMinScopeDuration()) >= 0
                            && remainingTaskDurations.get(t).isPositive()
                            && (t.getStartAt() == null || t.getStartAt().isBefore(slot.endAt()))).toList()
        );
        if (possibleTasks.isEmpty()) {
            log.debug("No task can start in remaining slot time; advancing to next slot");
            return advanceToNextSlot(tasks, dependencyCount, remainingTaskDurations, slots, slot, currentTime, plannedScopes);
        }

        for (WeightDataProvider provider : this.providers) {
            provider.calculate(new DataProviderContext(currentTime, plannedScopes), possibleTasks);
        }
        possibleTasks.sort((t1, t2) -> -1*Double.compare(getWeight(t1), getWeight(t2)));

        for (TaskGraphNode task : new ArrayList<>(possibleTasks)) {
            log.debug("Chose Task: {}", task);
            Instant effectiveStart = task.getStartAt().isAfter(currentTime) ? task.getStartAt() : currentTime;
            Duration remainingTaskSlotDuration = Duration.between(effectiveStart, slot.endAt());

            List<Scope> scopes = new ArrayList<>();

            Duration remainingTaskDuration = remainingTaskDurations.get(task);

            // Cap at maxScopeDuration after taking the smaller of remaining task and remaining slot
            Duration scopeDuration = remainingTaskDuration.compareTo(remainingSlotDuration) <= 0
                    ? remainingTaskDuration
                    : remainingTaskSlotDuration;
            if (scopeDuration.compareTo(task.getMaxScopeDuration()) > 0) {
                scopeDuration = task.getMaxScopeDuration();
            }

            // If the scope is too small continue
            if (scopeDuration.compareTo(task.getMinScopeDuration()) < 0) {
                continue;
            }

            // Tail guard: if the leftover after this scope is non-zero but below minScopeDuration,
            // shrink the current scope so the tail is exactly minScopeDuration
            Duration tentativeRemaining = remainingTaskDuration.minus(scopeDuration);
            if (!tentativeRemaining.isZero() && tentativeRemaining.compareTo(task.getMinScopeDuration()) < 0) {
                Duration shortfall = task.getMinScopeDuration().minus(tentativeRemaining);
                Duration adjustedScope = scopeDuration.minus(shortfall);
                if (adjustedScope.compareTo(task.getMinScopeDuration()) < 0) {
                    // No valid split exists here; try the next candidate task
                    continue;
                }
                scopeDuration = adjustedScope;
            }

            Duration newRemainingTaskDuration = remainingTaskDurations.get(task).minus(scopeDuration);

            remainingTaskDurations.put(task, newRemainingTaskDuration);
            // Only calculate new possible tasks if current one has been completly planned
            if (newRemainingTaskDuration.isZero()) {
                tasks.remove(task);
                for (TaskGraphNode successor : task.getUncompletedDependents()) {
                    int newCount = dependencyCount.get(successor) - 1;
                    dependencyCount.put(successor, newCount);
                    if (newCount == 0) {
                        tasks.add(successor);
                    }
                }
                System.out.println("Updated Tasks: " + tasks);
            }

            scopes.add(new Scope(null, task.task(), effectiveStart, effectiveStart.plus(scopeDuration)));

            if (tasks.isEmpty()) {
                return scopes;
            }

            Instant newCurrentTime = effectiveStart.plus(scopeDuration);
            List<Scope> nextResult;
            if (newCurrentTime.equals(slot.endAt())) {
                nextResult = advanceToNextSlot(tasks, dependencyCount, remainingTaskDurations,
                        slots, slot, newCurrentTime, scopes);
            } else {
                nextResult = plan(tasks, dependencyCount, remainingTaskDurations,
                        slots, slot, newCurrentTime, scopes);
            }
            if (nextResult != null) {
                scopes.addAll(nextResult);
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
        log.debug("Path did not find result");
        return advanceToNextSlot(tasks, dependencyCount, remainingTaskDurations, slots, slot, currentTime, plannedScopes);
    }

    private List<Scope> advanceToNextSlot(List<TaskGraphNode> tasks,
                                          Map<TaskGraphNode, Integer> dependencyCount,
                                          Map<TaskGraphNode, Duration> remainingTaskDurations,
                                          WorkSlotProvider slots,
                                          ConcreteWorkSlot slot,
                                          Instant currentTime,
                                          List<Scope> plannedScopes) {
        ConcreteWorkSlot nextSlot = slots.getNextSlot(slot);
        if (nextSlot == null || nextSlot.startAt().isBefore(currentTime)) {
            return null;
        }
        return plan(tasks, dependencyCount, remainingTaskDurations, slots, nextSlot, nextSlot.startAt(), plannedScopes);
    }

    private double getWeight(TaskGraphNode task) {
        double weight = 0;
        for (WeightDataProvider provider : this.providers) {
            weight += provider.getWeight(task);
        }
        return weight;
    }

}

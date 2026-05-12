package de.ni0.chronoscope.algorithm;

import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Critical Path Method calculator for a dynamic-task dependency graph.
 *
 * <p>The planner uses CPM slack as a weighting signal so tasks with less scheduling freedom
 * are tried earlier.</p>
 */
public class CPM {
    /**
     * Earliest/latest timing data calculated for one task graph node.
     */
    public record TaskData(TaskGraphNode task, Instant earliestStart, Instant earliestFinish, Instant latestStart, Instant latestFinish) {
        /**
         * Returns the difference between earliest and latest start.
         *
         * @return available scheduling slack for the task
         */
        public Duration getSlack() {
            return this.earliestStart.until(this.latestStart);
        }
    }

    @Getter
    private final Map<TaskGraphNode, TaskData> taskData = new HashMap<>();

    private Instant calcFirstStart(TaskGraphNode task, Instant defaultStart) {
        Instant firstStart = defaultStart;
        if (task.getStartAt() != null && task.getStartAt().isAfter(defaultStart)) {
            firstStart = task.getStartAt();
        }
        for (TaskGraphNode dependency : task.dependencies()) {
            Instant depEarliestStart = this.calcFirstStart(dependency, defaultStart);
            Instant depEarliestEnd = depEarliestStart.plus(dependency.getRemaining());
            if (depEarliestEnd.isAfter(firstStart)) {
                firstStart = depEarliestEnd;
            }
        }
        return firstStart;
    }
    private Instant calcLatestFinish(TaskGraphNode task) {
        Instant latestFinish = Instant.MAX;
        if (task.getEndAt() != null) {
            latestFinish = task.getEndAt();
        }
        for (TaskGraphNode successor : task.dependents()) {
            Instant sucLatestFinish = this.calcLatestFinish(successor);
            Instant sucLatestStart = sucLatestFinish.minus(successor.getRemaining());
            if (sucLatestStart.isBefore(latestFinish)) {
                latestFinish = sucLatestStart;
            }
        }
        return latestFinish;
    }

    /**
     * Calculates CPM timing data for the provided graph nodes.
     *
     * @param tasks task graph nodes to analyze
     * @param start default start time used for tasks with no dependency-imposed start
     */
    public CPM(List<TaskGraphNode> tasks, Instant start) {
        for (TaskGraphNode task : tasks) {
            Instant firstStart = calcFirstStart(task, start);
            Instant firstFinish = firstStart.plus(task.getRemaining());
            Instant latestFinish = calcLatestFinish(task);
            Instant latestStart = latestFinish.minus(task.getRemaining());
            this.taskData.put(task, new TaskData(task, firstStart, firstFinish, latestStart, latestFinish));
        }
    }
}

package de.ni0.chronoscope.algorithm;

import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CPM {
    public record TaskData(TaskGraphNode task, Instant earliestStart, Instant earliestFinish, Instant latestStart, Instant latestFinish) {
        public Duration getSlack() {
            return this.earliestStart.until(this.latestStart);
        }
    }

    @Getter
    private final Map<TaskGraphNode, TaskData> taskData = new HashMap<>();

    private Instant calcFirstStart(TaskGraphNode task, Instant defaultStart) {
        Instant firstStart = defaultStart;
        for (TaskGraphNode dependency : task.dependencies()) {
            Instant depEarliestStart = this.calcFirstStart(dependency, defaultStart);
            Instant depEarliestEnd = depEarliestStart.plus(dependency.getDuration());
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
            Instant sucLatestStart = sucLatestFinish.minus(successor.getDuration());
            if (sucLatestStart.isBefore(latestFinish)) {
                latestFinish = sucLatestStart;
            }
        }
        return latestFinish;
    }

    public CPM(List<TaskGraphNode> tasks, Instant start) {
        for (TaskGraphNode task : tasks) {
            Instant firstStart = calcFirstStart(task, start);
            Instant firstFinish = firstStart.plus(task.getDuration());
            Instant latestFinish = calcLatestFinish(task);
            Instant latestStart = latestFinish.minus(task.getDuration());
            this.taskData.put(task, new TaskData(task, firstStart, firstFinish, latestStart, latestFinish));
        }
    }
}

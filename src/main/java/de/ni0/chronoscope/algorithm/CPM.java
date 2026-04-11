package de.ni0.chronoscope.algorithm;

import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CPM {
    public record TaskData(Task task, Instant earliestStart, Instant earliestFinish, Instant latestStart, Instant latestFinish) {
        Duration getSlack() {
            return this.earliestStart.until(this.latestStart);
        }
    }

    @Getter
    private final Map<Task, TaskData> taskData = new HashMap<>();

    private Instant calcFirstStart(Task task, Instant defaultStart) {
        Instant firstStart = defaultStart;
        for (Task dependency : task.dependencies()) {
            Instant depEarliestStart = this.calcFirstStart(dependency, defaultStart);
            Instant depEarliestEnd = depEarliestStart.plus(dependency.duration());
            if (depEarliestEnd.isAfter(firstStart)) {
                firstStart = depEarliestEnd;
            }
        }
        return firstStart;
    }
    private Instant calcLatestFinish(Task task) {
        Instant latestFinish = Instant.MAX;
        if (task.end() != null) {
            latestFinish = task.end();
        }
        for (Task successor : task.successors()) {
            Instant sucLatestFinish = this.calcLatestFinish(successor);
            Instant sucLatestStart = sucLatestFinish.minus(successor.duration());
            if (sucLatestStart.isBefore(latestFinish)) {
                latestFinish = sucLatestStart;
            }
        }
        return latestFinish;
    }

    public CPM(List<Task> tasks, Instant start) {
        for (Task task : tasks) {
            Instant firstStart = calcFirstStart(task, start);
            Instant firstFinish = firstStart.plus(task.duration());
            Instant latestFinish = calcLatestFinish(task);
            Instant latestStart = latestFinish.minus(task.duration());
            this.taskData.put(task, new TaskData(task, firstStart, firstFinish, latestStart, latestFinish));
        }
    }
}

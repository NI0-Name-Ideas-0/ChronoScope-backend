package de.ni0.chronoscope.algorithm;

import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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

    public CPM(List<Task> tasks, Instant start) {
        List<Task> sorted = this.sort(tasks);

        for (Task task : sorted) {
            Instant earliestStart = start;
            for (Task dependency : task.dependencies()) {
                Instant dependencyEF = this.taskData.get(dependency).earliestFinish;
                if (dependencyEF.isAfter(earliestStart)) {
                    earliestStart = dependencyEF;
                }
            }
            Instant earliestFinish = earliestStart.plus(task.duration());
            this.taskData.put(task, new TaskData(task, earliestStart, earliestFinish, null, null));
        }

        for (Task task : sorted.reversed()) {
            Instant latestFinish = task.end();
            for (Task successor : task.successors()) {
                Instant dependencyLS = this.taskData.get(successor).latestStart;
                if (dependencyLS.isBefore(latestFinish)) {
                    latestFinish = dependencyLS;
                }
            }
            Instant latestStart = latestFinish.minus(task.duration());
            TaskData taskData = this.taskData.get(task);
            this.taskData.put(task, new TaskData(task, taskData.earliestStart, taskData.earliestFinish, latestStart, latestFinish));
        }
    }

    private List<Task> sort(List<Task> tasks) {
        List<Task> sorted = new ArrayList<>();

        List<Task> remainingTasks = new ArrayList<>(tasks);
        while (!remainingTasks.isEmpty()) {
            Task task = remainingTasks.getFirst();
            visit(task, new ArrayList<>(), sorted);
            remainingTasks.remove(task);
        }

        return sorted;
    }

    private void visit(Task task, List<Task> marked, List<Task> sorted) {
        if (sorted.contains(task)) {
            return;
        }
        if (marked.contains(task)) {
            throw new IllegalStateException("Cycle detected");
        }
        marked.add(task);

        for (Task dependency : task.dependencies()) {
            visit(dependency, marked, sorted);
        }
        sorted.add(task);
    }
}

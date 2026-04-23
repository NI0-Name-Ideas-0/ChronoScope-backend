package de.ni0.chronoscope.algorithm;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record Task(de.ni0.chronoscope.model.DynamicTask task, List<Task> dependencies, List<Task> dependents) {

    @Override
    public String toString() {
        return Long.toString(this.task.getId());
    }

    public Instant start() {
        return this.task.getStartAt();
    }

    public Instant end() {
        return this.task.getEndAt();
    }

    public Duration duration() {
        return this.task.getDuration();
    }
}

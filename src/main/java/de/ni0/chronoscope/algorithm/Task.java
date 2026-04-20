package de.ni0.chronoscope.algorithm;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record Task(de.ni0.chronoscope.model.DynamicTask task, List<Task> dependencies, List<Task> successors) {

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return Objects.equals(task.task.getId(), this.task.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(task.getId());
    }

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

package de.ni0.chronoscope.algorithm;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import de.ni0.chronoscope.model.DynamicTask;

public record TaskGraphNode(DynamicTask task, List<TaskGraphNode> dependencies, List<TaskGraphNode> dependents) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskGraphNode other)) return false;
        return Objects.equals(this.task.getId(), other.task.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.task.getId());
    }

    @Override
    public String toString() {
        return Objects.toString(this.task.getId(), "null");
    }

    public Instant getStartAt() {
        return this.task.getStartAt();
    }

    public Instant getEndAt() {
        return this.task.getEndAt();
    }

    public Duration getDuration() {
        return this.task.getDuration();
    }
}

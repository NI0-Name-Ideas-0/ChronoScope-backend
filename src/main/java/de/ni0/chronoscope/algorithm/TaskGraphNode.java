package de.ni0.chronoscope.algorithm;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import de.ni0.chronoscope.model.DynamicTask;

public record TaskGraphNode(DynamicTask task, List<TaskGraphNode> dependencies, List<TaskGraphNode> dependents) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskGraphNode other)) return false;
        return this.task.getId() == other.task.getId();
    }

    @Override
    public int hashCode() {
        return Long.hashCode(this.task.getId());
    }

    @Override
    public String toString() {
        return Long.toString(this.task.getId());
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

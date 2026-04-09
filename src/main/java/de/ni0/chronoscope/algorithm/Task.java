package de.ni0.chronoscope.algorithm;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record Task(int id, int complexity, Duration duration, Instant start, Instant end, List<Task> dependencies, List<Task> successors) {

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return task.id == this.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return Integer.toString(this.id);
    }
}

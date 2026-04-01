package de.ni0.chronoscope.model;

import lombok.Data;

import java.time.Duration;
import java.time.Instant;

@Data
public class Scope {
    private String taskName;
    private Instant start;
    private Duration duration;

    public Scope(String taskName, Instant start, Duration duration) {
        this.taskName = taskName;
        this.start = start;
        this.duration = duration;
    }
}

package de.ni0.chronoscope.model;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class DynamicTask extends Task {
    private Instant start;
    private Instant end;
    private Duration duration;
    private int complexity;
    private List<DynamicTask> dependencies = new ArrayList<>();

    public DynamicTask(String name, String description,
                       Instant start, Instant end, Duration duration,
                       int complexity) {
        super(name, description);
        this.start = start;
        this.end = end;
        this.duration = duration;
        this.complexity = complexity;
    }
}

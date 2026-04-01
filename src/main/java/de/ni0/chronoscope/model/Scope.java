package de.ni0.chronoscope.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Duration;
import java.time.Instant;

@Entity
@Data
public class Scope {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Task task;
    private Instant start;
    private Duration duration;

    public Scope(Task task, Instant start, Duration duration) {
        this.task = task;
        this.start = start;
        this.duration = duration;
    }

    public Scope() {

    }
}

package de.ni0.chronoscope.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.Duration;

@Data
@Entity
public class Task {
    @Id
    @GeneratedValue
    private Long id;

    private String name;
    private Duration duration;
}

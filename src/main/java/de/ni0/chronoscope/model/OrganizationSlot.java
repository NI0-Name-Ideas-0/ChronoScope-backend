package de.ni0.chronoscope.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.Duration;
import java.time.Instant;

@Entity
@Data
public class OrganizationSlot {
    @Id
    @GeneratedValue
    private Long id;

    private Instant start;
    private Duration duration;
}

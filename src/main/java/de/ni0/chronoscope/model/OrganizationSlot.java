package de.ni0.chronoscope.model;

import lombok.Data;

import java.time.Duration;
import java.time.Instant;

@Data
public class OrganizationSlot {
    private Instant start;
    private Duration duration;
}

package de.ni0.chronoscope.controller.dto;

import lombok.Data;

import java.time.Duration;
import java.time.Instant;

@Data
public class CreateOrganizationSlotRequest {
    private Instant start;
    private Duration duration;
}

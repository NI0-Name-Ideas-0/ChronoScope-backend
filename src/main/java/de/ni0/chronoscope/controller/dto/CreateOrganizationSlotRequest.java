package de.ni0.chronoscope.controller.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Duration;
import java.time.Instant;

@Data
public class CreateOrganizationSlotRequest {
    @NotNull
    private Instant start;
    @NotNull
    private Duration duration;
}

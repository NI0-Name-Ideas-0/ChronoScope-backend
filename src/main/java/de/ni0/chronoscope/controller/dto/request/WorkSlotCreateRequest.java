package de.ni0.chronoscope.controller.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Request payload for creating a recurring weekly availability window.
 */
public record WorkSlotCreateRequest(
    @NotNull String organizationId,
    @NotNull DayOfWeek dayOfWeek,
    @NotNull LocalTime startTime,
    @NotNull LocalTime endTime
) {
}

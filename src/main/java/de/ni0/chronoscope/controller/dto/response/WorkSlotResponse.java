package de.ni0.chronoscope.controller.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * API representation of a recurring weekly availability window.
 */
public record WorkSlotResponse(
    @NotNull Long id,
    @NotNull String organizationId,
    @NotNull DayOfWeek dayOfWeek,
    @NotNull @JsonFormat(pattern = "HH:mm") LocalTime startTime,
    @NotNull @JsonFormat(pattern = "HH:mm") LocalTime endTime
) {
}

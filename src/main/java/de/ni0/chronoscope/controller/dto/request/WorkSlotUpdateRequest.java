package de.ni0.chronoscope.controller.dto.request;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Partial update request for a recurring weekly availability window.
 */
public record WorkSlotUpdateRequest(
    DayOfWeek dayOfWeek,
    LocalTime startTime,
    LocalTime endTime
) {
}

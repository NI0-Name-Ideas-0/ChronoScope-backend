package de.ni0.chronoscope.controller.dto;

import java.time.Instant;

public record WorkSlotDto(
    Long id,
    Long accountId,
    Instant start,
    Instant end
) {
}

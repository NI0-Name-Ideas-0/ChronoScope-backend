package de.ni0.chronoscope.controller.dto.response;

import java.time.Instant;

/**
 * API representation of an availability window.
 */
public record WorkSlotResponse(
    Long id,
    String organizationId,
    Instant startAt,
    Instant endAt
) {
}

package de.ni0.chronoscope.controller.dto.request;

import java.time.Instant;

/**
 * Partial update request for an availability window.
 */
public record WorkSlotUpdateRequest(
    Instant startAt,
    Instant endAt
) {
}

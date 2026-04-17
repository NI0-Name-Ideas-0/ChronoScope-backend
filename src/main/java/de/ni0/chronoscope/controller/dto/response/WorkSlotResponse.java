package de.ni0.chronoscope.controller.dto.response;

import java.time.Instant;

public record WorkSlotResponse(
    Long id,
    Long accountId,
    Instant startAt,
    Instant endAt
) {
}

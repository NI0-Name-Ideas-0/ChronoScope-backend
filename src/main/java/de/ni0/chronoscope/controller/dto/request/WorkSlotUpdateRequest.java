package de.ni0.chronoscope.controller.dto.request;

import java.time.Instant;

public record WorkSlotUpdateRequest(
    Instant startAt,
    Instant endAt
) {
}

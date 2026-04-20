package de.ni0.chronoscope.controller.dto.response;

import java.time.Instant;

public record ScopeResponse(
    Long id,
    Long dynamicTaskId,
    Instant startAt,
    Instant endAt
) {
}

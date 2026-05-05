package de.ni0.chronoscope.controller.dto.response;

import java.time.Instant;

/**
 * API representation of a planned dynamic-task time window.
 */
public record ScopeResponse(
    Long id,
    Long dynamicTaskId,
    Instant startAt,
    Instant endAt
) {
}

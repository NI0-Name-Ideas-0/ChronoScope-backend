package de.ni0.chronoscope.controller.dto.response;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * API representation of a planned dynamic-task time window.
 */
public record ScopeResponse(
    @NotNull Long id,
    @NotNull Long dynamicTaskId,
    @NotNull Instant startAt,
    @NotNull Instant endAt
) {
}

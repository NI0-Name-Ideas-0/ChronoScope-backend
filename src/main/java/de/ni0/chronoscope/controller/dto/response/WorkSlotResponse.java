package de.ni0.chronoscope.controller.dto.response;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * API representation of an availability window.
 */
public record WorkSlotResponse(
    @NotNull Long id,
    @NotNull String organizationId,
    @NotNull Instant startAt,
    @NotNull Instant endAt
) {
}

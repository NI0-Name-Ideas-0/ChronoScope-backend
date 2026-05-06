package de.ni0.chronoscope.controller.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Request payload for creating an availability window.
 */
public record WorkSlotCreateRequest(
    @NotNull Long organizationId,
    @NotNull Instant startAt,
    @NotNull Instant endAt
) {
}

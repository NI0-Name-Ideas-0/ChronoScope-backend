package de.ni0.chronoscope.controller.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record WorkSlotCreateRequest(
    @NotNull Long accountId,
    @NotNull Long organizationId,
    @NotNull Instant startAt,
    @NotNull Instant endAt
) {
}

package de.ni0.chronoscope.controller.dto.response;

import jakarta.validation.constraints.NotNull;

/**
 * API representation of a linked login account and its accessible organizations.
 */
public record AccountResponse(
    @NotNull Long id,
    @NotNull Long identityId,
    @NotNull String mail
) {
}

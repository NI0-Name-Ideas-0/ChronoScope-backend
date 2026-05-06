package de.ni0.chronoscope.controller.dto.response;

import jakarta.validation.constraints.NotNull;

/**
 * Response returned after two accounts have been merged under one identity.
 */
public record AccountLinkConfirmResponse(
    @NotNull Long sourceAccountId,
    @NotNull Long targetAccountId,
    @NotNull String status
) {
}

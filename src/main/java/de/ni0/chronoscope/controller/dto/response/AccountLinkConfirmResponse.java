package de.ni0.chronoscope.controller.dto.response;

/**
 * Response returned after two accounts have been merged under one identity.
 */
public record AccountLinkConfirmResponse(
    Long sourceAccountId,
    Long targetAccountId,
    String status
) {
}

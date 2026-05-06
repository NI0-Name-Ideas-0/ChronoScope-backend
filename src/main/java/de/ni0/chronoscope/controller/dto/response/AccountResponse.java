package de.ni0.chronoscope.controller.dto.response;

/**
 * API representation of a linked login account and its accessible organizations.
 */
public record AccountResponse(
    Long id,
    Long identityId
) {
}

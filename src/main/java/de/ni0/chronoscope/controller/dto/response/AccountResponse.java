package de.ni0.chronoscope.controller.dto.response;

import java.util.List;

/**
 * API representation of a linked login account and its accessible organizations.
 */
public record AccountResponse(
    Long id,
    Long identityId,
    List<OrganizationResponse> organizations
) {
}

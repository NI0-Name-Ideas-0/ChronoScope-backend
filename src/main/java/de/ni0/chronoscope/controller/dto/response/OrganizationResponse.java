package de.ni0.chronoscope.controller.dto.response;

import jakarta.validation.constraints.NotNull;

/**
 * API representation of an organizationId.
 */
public record OrganizationResponse(
    @NotNull String id,
    @NotNull String name
) {
}

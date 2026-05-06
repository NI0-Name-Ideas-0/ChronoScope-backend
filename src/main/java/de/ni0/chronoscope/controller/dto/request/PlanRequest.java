package de.ni0.chronoscope.controller.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Request payload selecting the account and organization to plan.
 */
public record PlanRequest(
    @NotNull String organizationId
) {
}

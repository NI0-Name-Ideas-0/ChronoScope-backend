package de.ni0.chronoscope.controller.dto.request;

/**
 * Request payload selecting the account and organizationId to plan.
 */
public record PlanRequest(
    String organizationId
) {
}

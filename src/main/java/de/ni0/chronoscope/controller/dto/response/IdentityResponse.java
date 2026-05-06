package de.ni0.chronoscope.controller.dto.response;

import java.util.List;
import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * API representation of the authenticated identity and all linked accounts.
 */
public record IdentityResponse(
    @NotNull Long id,
    @NotNull List<AccountResponse> accounts,
    @Schema(description = "Organization names for which the authenticated identity has admin privileges, derived from JWT groups below /org-admin.")
    @NotNull Set<String> adminOrganizations,
    @Schema(description = "Organizations to which the identity belongs.")
    @NotNull Set<Organization> organizations
) {
    public record Organization(
        String name,
        String id
    ){}
}

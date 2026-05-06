package de.ni0.chronoscope.controller.dto.response;

import java.util.List;
import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * API representation of the authenticated identity and all linked accounts.
 */
public record IdentityResponse(
    Long id,
    List<AccountResponse> accounts,
    @Schema(description = "Organization names for which the authenticated identity has admin privileges, derived from JWT groups below /org-admin.")
    Set<String> adminOrganizations,
    @Schema(description = "Organizations to which the identity belongs.")
    Set<Organization> organizations
) {
    public record Organization(
        String name,
        String id
    ){}
}

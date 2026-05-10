package de.ni0.chronoscope.controller.dto.response;

import java.util.List;
import java.util.Set;

import de.ni0.chronoscope.model.WorkSettings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * API representation of the authenticated identity and all linked accounts.
 */
public record IdentityResponse(
    @NotNull Long id,
    @NotNull List<AccountResponse> accounts,
    @Schema(description = "Organization IDs for which the authenticated identity has admin privileges, resolved server-side via Keycloak Admin Client lookups.")
    @NotNull Set<String> adminOrganizations,
    @Schema(description = "Organizations to which the identity belongs.")
    @NotNull Set<Organization> organizations,
    @Schema(description = "Language preference (de_DE or en_US)")
    @NotNull String language,
    @Schema(description = "Theme preference (light, dark, or system)")
    @NotNull String theme,
    @Schema(description = "Opaque work schedule settings stored as JSON.")
    WorkSettings workSettings
) {
    public record Organization(
        @NotNull String name,
        @NotNull String id
    ){}
}

package de.ni0.chronoscope.controller.dto.response;

import de.ni0.chronoscope.model.ColorToken;
import jakarta.validation.constraints.NotNull;

public record IdentityOrganizationColorResponse(
    @NotNull String organizationId,
    @NotNull ColorToken color
) {
}

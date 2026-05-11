package de.ni0.chronoscope.controller.dto.request;

import de.ni0.chronoscope.model.ColorToken;
import jakarta.validation.constraints.NotNull;

public record IdentityOrganizationColorUpdateRequest(
    @NotNull ColorToken color
) {
}

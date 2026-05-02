package de.ni0.chronoscope.controller.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record IdentityResponse(
    Long id,
    List<AccountResponse> accounts,
    @Schema(description = "Organization names from token groups below /org-admin.")
    List<String> adminOrganizations
) {
}

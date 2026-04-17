package de.ni0.chronoscope.controller.dto.response;

import java.util.List;

public record AccountResponse(
    Long id,
    Long identityId,
    List<OrganizationResponse> organizations
) {
}

package de.ni0.chronoscope.controller.dto;

import java.util.List;

public record AccountDetailedDto(
    Long id,
    Long identityId,
    List<OrganizationDto> organizations
) {
}

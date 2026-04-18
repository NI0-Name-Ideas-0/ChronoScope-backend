package de.ni0.chronoscope.mapper;

import de.ni0.chronoscope.controller.dto.response.OrganizationResponse;
import de.ni0.chronoscope.model.Organization;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrganizationMapper {

    OrganizationResponse toResponse(Organization organization);
}
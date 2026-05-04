package de.ni0.chronoscope.mapper;

import de.ni0.chronoscope.controller.dto.response.OrganizationResponse;
import de.ni0.chronoscope.model.Organization;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for organizations.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrganizationMapper {

    /**
     * Converts an organization entity to its API representation.
     *
     * @param organization organization entity
     * @return response DTO
     */
    OrganizationResponse toResponse(Organization organization);
}

package de.ni0.chronoscope.mapper;

import java.util.Set;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import de.ni0.chronoscope.controller.dto.response.IdentityResponse;
import de.ni0.chronoscope.model.Identity;

/**
 * MapStruct mapper for identity responses.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = AccountMapper.class)
public interface IdentityMapper {

    /**
     * Converts an identity plus request-specific admin organizations to the API response.
     *
     * @param identity loaded identity entity
     * @param adminOrganizations organizationId names where the current account has admin privileges
     * @return response DTO
     */
    @Mapping(target = "id", source = "identity.id")
    @Mapping(target = "accounts", source = "identity.accounts")
    @Mapping(target = "adminOrganizations", source = "adminOrganizations")
    @Mapping(target = "organizations", source = "organizations")
    IdentityResponse toResponse(Identity identity, Set<String> adminOrganizations, Set<IdentityResponse.Organization> organizations);
}

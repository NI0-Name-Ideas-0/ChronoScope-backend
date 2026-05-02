package de.ni0.chronoscope.mapper;

import java.util.List;

import de.ni0.chronoscope.controller.dto.response.IdentityResponse;
import de.ni0.chronoscope.model.Identity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = AccountMapper.class)
public interface IdentityMapper {

    @Mapping(target = "id", source = "identity.id")
    @Mapping(target = "accounts", source = "identity.accounts")
    @Mapping(target = "adminOrganizations", source = "adminOrganizations")
    IdentityResponse toResponse(Identity identity, List<String> adminOrganizations);
}

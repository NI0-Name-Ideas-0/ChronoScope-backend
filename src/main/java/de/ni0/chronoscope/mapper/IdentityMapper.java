package de.ni0.chronoscope.mapper;

import de.ni0.chronoscope.controller.dto.response.IdentityResponse;
import de.ni0.chronoscope.model.Identity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = AccountMapper.class)
public interface IdentityMapper {

    IdentityResponse toResponse(Identity identity);
}

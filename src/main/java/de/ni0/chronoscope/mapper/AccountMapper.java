package de.ni0.chronoscope.mapper;

import de.ni0.chronoscope.controller.dto.response.AccountResponse;
import de.ni0.chronoscope.model.Account;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = OrganizationMapper.class)
public interface AccountMapper {

    @Mapping(target = "identityId", source = "identity.id")
    AccountResponse toAccountResponse(Account account);
}

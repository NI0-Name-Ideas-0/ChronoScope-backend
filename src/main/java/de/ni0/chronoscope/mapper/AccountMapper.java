package de.ni0.chronoscope.mapper;

import de.ni0.chronoscope.controller.dto.response.AccountResponse;
import de.ni0.chronoscope.model.Account;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for account API responses.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AccountMapper {

    /**
     * Converts an account entity to its response DTO, exposing the owning identity ID.
     *
     * @param account account entity
     * @return response DTO
     */
    @Mapping(target = "identityId", source = "identity.id")
    AccountResponse toResponse(Account account);
}

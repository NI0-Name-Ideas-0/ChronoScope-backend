package de.ni0.chronoscope.mapper;

import de.ni0.chronoscope.controller.dto.response.ScopeResponse;
import de.ni0.chronoscope.model.Scope;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for planned scopes.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ScopeMapper {

    /**
     * Converts a planned scope to its API representation.
     *
     * @param scope planned scope entity
     * @return response DTO
     */
    @Mapping(target = "dynamicTaskId", source = "dynamicTask.id")
    ScopeResponse toResponse(Scope scope);
}

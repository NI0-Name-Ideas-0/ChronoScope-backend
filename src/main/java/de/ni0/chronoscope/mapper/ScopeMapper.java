package de.ni0.chronoscope.mapper;

import de.ni0.chronoscope.controller.dto.response.ScopeResponse;
import de.ni0.chronoscope.model.Scope;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ScopeMapper {

    @Mapping(target = "dynamicTaskId", source = "dynamicTask.id")
    ScopeResponse toResponse(Scope scope);
}

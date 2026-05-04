package de.ni0.chronoscope.mapper;

import de.ni0.chronoscope.controller.dto.request.LabelCreateRequest;
import de.ni0.chronoscope.controller.dto.response.LabelResponse;
import de.ni0.chronoscope.model.Label;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for task labels.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)  
public interface LabelMapper {

    /**
     * Converts a label creation DTO into an unattached label entity.
     *
     * @param request label creation payload
     * @return new label entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "task", ignore = true)
    Label fromCreateRequest(LabelCreateRequest request);

    /**
     * Converts a label entity to its API representation.
     *
     * @param label label entity
     * @return response DTO
     */
    @Mapping(target = "taskId", source = "task.id")
    LabelResponse toResponse(Label label);
}

package de.ni0.chronoscope.mapper;

import de.ni0.chronoscope.controller.dto.request.LabelCreateRequest;
import de.ni0.chronoscope.controller.dto.response.LabelResponse;
import de.ni0.chronoscope.model.Label;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LabelMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "task", ignore = true)
    Label fromCreateRequest(LabelCreateRequest request);

    @Mapping(target = "taskId", source = "task.id")
    LabelResponse toResponse(Label label);
}

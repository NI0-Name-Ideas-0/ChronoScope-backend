package de.ni0.chronoscope.mapper;

import de.ni0.chronoscope.controller.dto.request.TaskDependencyCreateRequest;
import de.ni0.chronoscope.controller.dto.response.TaskDependencyResponse;
import de.ni0.chronoscope.model.TaskDependency;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = TaskProxyProvider.class)
public interface TaskDependencyMapper {

    @Mapping(target = "id", ignore = true) // will be filled in by database
    @Mapping(target = "dynamicTask", ignore = true) // will have to be filled manually
    @Mapping(target = "predecessor", source = "predecessorDynamicTaskId", qualifiedByName = "predecessorProxy")
    TaskDependency fromCreateRequest(TaskDependencyCreateRequest request);

    @Mapping(target = "dynamicTaskId", source = "dynamicTask.id")
    @Mapping(target = "predecessorDynamicTaskId", source = "predecessor.id")
    TaskDependencyResponse toResponse(TaskDependency dependency);
}

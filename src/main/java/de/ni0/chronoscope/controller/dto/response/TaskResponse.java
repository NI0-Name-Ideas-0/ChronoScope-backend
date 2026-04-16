package de.ni0.chronoscope.controller.dto.response;

import de.ni0.chronoscope.controller.dto.DynamicTaskDto;
import de.ni0.chronoscope.controller.dto.StaticTaskDto;
import de.ni0.chronoscope.controller.dto.TagDto;
import de.ni0.chronoscope.controller.dto.TaskDependencyDto;

import java.util.List;

public record TaskResponse(
    Long id,
    Long accountId,
    String name,
    String description,
    String rrule,
    DynamicTaskDto dynamicTask,
    StaticTaskDto staticTask,
    List<LabelDto> labels,
    List<TaskDependencyDto> dependencies
) {
}

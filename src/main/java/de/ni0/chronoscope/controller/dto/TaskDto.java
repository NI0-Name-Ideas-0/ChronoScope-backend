package de.ni0.chronoscope.controller.dto;

import java.util.List;

public record TaskDto(
    Long id,
    Long accountId,
    String name,
    String description,
    String rrule,
    DynamicTaskDto dynamicTask,
    StaticTaskDto staticTask,
    List<TagDto> tags,
    List<TaskDependencyDto> dependencies
) {
}

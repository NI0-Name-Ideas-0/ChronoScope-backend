package de.ni0.chronoscope.controller.dto;

import java.util.List;

public record DynamicTaskDto(
    Long id,
    Integer duration,
    Integer elapsed,
    Integer minScopeDuration,
    Integer maxScopeDuration,
    List<ScopeDto> scopes,
    List<TaskDependencyDto> dependencies
) {
}

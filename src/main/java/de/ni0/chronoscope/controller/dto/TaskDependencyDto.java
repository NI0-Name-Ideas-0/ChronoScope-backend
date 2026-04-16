package de.ni0.chronoscope.controller.dto;

public record TaskDependencyDto(
    Long id,
    Long taskId,
    Long predecessorTaskId
) {
}

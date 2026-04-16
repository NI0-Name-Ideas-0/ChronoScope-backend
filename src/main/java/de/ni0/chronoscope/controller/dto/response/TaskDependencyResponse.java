package de.ni0.chronoscope.controller.dto.response;

public record TaskDependencyResponse(
    Long id,
    Long taskId,
    Long predecessorTaskId
) {
}

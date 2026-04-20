package de.ni0.chronoscope.controller.dto.response;

public record TaskDependencyResponse(
    Long id,
    Long dynamicTaskId,
    Long predecessorDynamicTaskId
) {
}

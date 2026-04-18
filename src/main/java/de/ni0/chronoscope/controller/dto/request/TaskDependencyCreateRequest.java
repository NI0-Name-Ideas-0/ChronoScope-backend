package de.ni0.chronoscope.controller.dto.request;

import jakarta.validation.constraints.NotNull;

public record TaskDependencyCreateRequest(
    @NotNull Long predecessorDynamicTaskId
) {
}

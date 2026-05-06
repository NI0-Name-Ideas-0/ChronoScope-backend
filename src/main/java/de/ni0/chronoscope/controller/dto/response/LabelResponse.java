package de.ni0.chronoscope.controller.dto.response;

import jakarta.validation.constraints.NotNull;

/**
 * API representation of a task label.
 */
public record LabelResponse(
    @NotNull Long id,
    @NotNull Long taskId,
    @NotNull String name
) {
}

package de.ni0.chronoscope.controller.dto.response;

/**
 * API representation of a task label.
 */
public record LabelResponse(
    Long id,
    Long taskId,
    String name
) {
}

package de.ni0.chronoscope.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for creating a task label.
 */
public record LabelCreateRequest(
    @NotBlank String name
) {
}

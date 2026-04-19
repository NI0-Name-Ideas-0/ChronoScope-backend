package de.ni0.chronoscope.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LabelCreateRequest(
    @NotBlank String name
) {
}

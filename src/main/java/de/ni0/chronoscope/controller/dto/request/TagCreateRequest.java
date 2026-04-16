package de.ni0.chronoscope.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TagCreateRequest(
    @NotBlank String name
) {
}

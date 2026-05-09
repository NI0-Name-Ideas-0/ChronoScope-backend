package de.ni0.chronoscope.controller.dto.request;

import java.util.Optional;

import jakarta.validation.constraints.Pattern;

/**
 * Request payload for updating user settings (language and theme preferences).
 */
public record SettingsUpdateRequest(
    @Pattern(regexp = "^(de_DE|en_US)$", message = "language must be one of: de_DE, en_US")
    Optional<String> language,
    @Pattern(regexp = "^(light|dark|system)$", message = "theme must be one of: light, dark, system")
    Optional<String> theme
) {
}

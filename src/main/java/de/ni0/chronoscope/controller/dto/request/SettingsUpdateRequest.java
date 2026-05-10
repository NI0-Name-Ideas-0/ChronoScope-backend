package de.ni0.chronoscope.controller.dto.request;

import java.util.Optional;

import de.ni0.chronoscope.model.WorkSettings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

/**
 * Request payload for updating user settings (language, theme, and work schedule preferences).
 */
public record SettingsUpdateRequest(
     Optional<@Pattern(regexp = "^(de_DE|en_US)$", message = "language must be one of: de_DE, en_US") String> language,
     Optional<@Pattern(regexp = "^(light|dark|system)$", message = "theme must be one of: light, dark, system") String> theme,
     Optional<@Valid WorkSettings> workSettings
) {
}

package de.ni0.chronoscope.controller.dto.response;

import de.ni0.chronoscope.model.WorkSettings;
import io.swagger.v3.oas.annotations.media.Schema;

public record SettingsResponse(
    @Schema(description = "Language preference (de_DE or en_US)") String language,
    @Schema(description = "Theme preference (light, dark, or system)") String theme,
    @Schema(description = "Opaque work schedule settings stored as JSON.") WorkSettings workSettings
) {}

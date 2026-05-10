package de.ni0.chronoscope.model;

import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Opaque persisted work schedule settings stored as JSON.
 */
public record WorkSettings(
    @NotNull @Min(1) Integer dailyWorkTimeMinutes,
    @NotNull @Size(min = 1) Set<@Pattern(regexp = "^(mo|di|mi|do|fr|sa|so)$", message = "work day must be one of: mo, di, mi, do, fr, sa, so") String> workDays
) {

    @Converter
    public static class WorkSettingsConverter implements AttributeConverter<WorkSettings, String> {

        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

        @Override
        public String convertToDatabaseColumn(WorkSettings attribute) {
            if (attribute == null) {
                return null;
            }

            try {
                return OBJECT_MAPPER.writeValueAsString(attribute);
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Failed to serialize work settings", exception);
            }
        }

        @Override
        public WorkSettings convertToEntityAttribute(String dbData) {
            if (dbData == null || dbData.isBlank()) {
                return null;
            }

            try {
                return OBJECT_MAPPER.readValue(dbData, WorkSettings.class);
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Failed to deserialize work settings", exception);
            }
        }
    }
}
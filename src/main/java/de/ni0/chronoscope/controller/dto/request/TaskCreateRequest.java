package de.ni0.chronoscope.controller.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record TaskCreateRequest(
    @NotNull Long accountId,
    @NotBlank String name,
    String description,
    String rrule,
    @NotNull TaskType type,
    // dynamic task fields
    Integer difficulty,
    Integer duration,
    Instant start,
    Instant end,
    Integer minScopeDuration,
    Integer maxScopeDuration,
    // static task fields
    Boolean blocker,
    List<TagCreateRequest> tags
) {
    public enum TaskType {
        @JsonProperty("dynamic") DYNAMIC,
        @JsonProperty("static") STATIC
    }
}

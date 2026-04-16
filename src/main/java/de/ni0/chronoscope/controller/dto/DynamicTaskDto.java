package de.ni0.chronoscope.controller.dto;

import java.time.Instant;

public record DynamicTaskDto(
    Long id,
    Integer difficulty,
    Integer duration,
    Integer elapsed,
    Instant start,
    Instant end,
    Integer minScopeDuration,
    Integer maxScopeDuration
) {
}

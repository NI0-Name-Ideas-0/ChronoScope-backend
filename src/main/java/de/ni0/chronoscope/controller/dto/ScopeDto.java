package de.ni0.chronoscope.controller.dto;

import java.time.Instant;

public record ScopeDto(
    Long id,
    Long taskId,
    Instant begin,
    Instant end
) {
}

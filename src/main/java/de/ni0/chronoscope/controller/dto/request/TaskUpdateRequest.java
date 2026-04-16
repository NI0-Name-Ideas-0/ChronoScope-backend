package de.ni0.chronoscope.controller.dto.request;

import java.time.Instant;

public record TaskUpdateRequest(
    String name,
    String description,
    String rrule,
    Integer difficulty,
    Integer duration,
    Integer elapsed,
    Instant start,
    Instant end,
    Boolean blocker
) {
}

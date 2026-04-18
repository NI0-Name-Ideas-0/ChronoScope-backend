package de.ni0.chronoscope.controller.dto.request;

import java.time.Instant;
import java.util.List;

public record DynamicTaskUpdateRequest(
    String name,
    String description,
    String rrule,
    Integer difficulty,
    Instant startAt,
    Instant endAt,
    List<LabelCreateRequest> labels,
    Integer duration,
    Integer elapsed,
    Integer minScopeDuration,
    Integer maxScopeDuration
) implements TaskUpdateRequest {
}

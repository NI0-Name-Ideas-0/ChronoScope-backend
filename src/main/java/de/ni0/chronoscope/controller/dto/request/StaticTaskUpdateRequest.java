package de.ni0.chronoscope.controller.dto.request;

import java.time.Instant;
import java.util.List;

public record StaticTaskUpdateRequest(
    String name,
    String description,
    String rrule,
    Integer difficulty,
    Instant startAt,
    Instant endAt,
    List<LabelCreateRequest> labels,
    Boolean isBlocker
) implements TaskUpdateRequest {
}

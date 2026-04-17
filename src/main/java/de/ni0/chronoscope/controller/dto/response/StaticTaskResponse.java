package de.ni0.chronoscope.controller.dto.response;

import java.time.Instant;
import java.util.List;

public record StaticTaskResponse(
    Long id,
    Long accountId,
    String name,
    String description,
    Integer difficulty,
    Instant startAt,
    Instant endAt,
    String rrule,
    List<LabelResponse> labels,
    Boolean isBlocker
) implements TaskResponse {
}

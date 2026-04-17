package de.ni0.chronoscope.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Update request for a static (fixed-time) task")
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

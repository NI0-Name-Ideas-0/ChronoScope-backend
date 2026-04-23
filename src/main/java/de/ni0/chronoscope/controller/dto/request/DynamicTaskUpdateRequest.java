package de.ni0.chronoscope.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Update request for a dynamic (schedulable) task")
public record DynamicTaskUpdateRequest(
    @Size(min = 1) String name,
    @Size(min = 1) String description,
    @Size(min = 1) String rrule,
    @Positive Integer difficulty,
    Instant startAt,
    Instant endAt,
    List<@Valid @NotNull LabelCreateRequest> labels,
    Duration duration,
    Duration elapsed,
    Duration minScopeDuration,
    Duration maxScopeDuration,
    List<@NotNull Long> dependencies
) implements TaskUpdateRequest {
}

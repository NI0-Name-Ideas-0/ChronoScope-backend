package de.ni0.chronoscope.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record DynamicTaskCreateRequest(
    @NotNull Long accountId,
    @NotBlank String name,
    String description,
    String rrule,
    Integer difficulty,
    Instant startAt,
    Instant endAt,
    List<LabelCreateRequest> labels,
    Integer duration,
    Integer minScopeDuration,
    Integer maxScopeDuration
) implements TaskCreateRequest {
}

package de.ni0.chronoscope.controller.dto.response;

import java.time.Instant;
import java.util.List;

public record DynamicTaskResponse(
    Long id,
    Long accountId,
    String name,
    String description,
    Integer difficulty,
    Instant startAt,
    Instant endAt,
    String rrule,
    List<LabelResponse> labels,
    Integer duration,
    Integer elapsed,
    Integer minScopeDuration,
    Integer maxScopeDuration,
    List<ScopeResponse> scopes,
    List<TaskDependencyResponse> dependencies
) implements TaskResponse {
}

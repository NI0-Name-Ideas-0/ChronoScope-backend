package de.ni0.chronoscope.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Schema(description = "Response for a dynamic (schedulable) task, including its scopes and dependencies")
public record DynamicTaskResponse(
    Long id,
    Long accountId,
    Long organizationId,
    String name,
    String description,
    Integer difficulty,
    Instant startAt,
    Instant endAt,
    String rrule,
    List<LabelResponse> labels,
    Duration duration,
    Duration elapsed,
    Duration minScopeDuration,
    Duration maxScopeDuration,
    List<ScopeResponse> scopes,
    List<Long> dependencies,
    List<Long> dependents
) implements TaskResponse {
}

package de.ni0.chronoscope.controller.dto.response;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import de.ni0.chronoscope.model.ColorToken;
import de.ni0.chronoscope.model.Task;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * API representation of a schedulable task, including generated scopes and graph links.
 */
@Schema(description = "Response for a dynamic (schedulable) task, including its scopes and dependencies")
public record DynamicTaskResponse(
    @NotNull Long id,
    @NotNull String organizationId,
    ColorToken color,
    @NotNull String name,
    String description,
    @NotNull Task.Difficulty difficulty,
    @NotNull Instant startAt,
    @NotNull Instant endAt,
    @NotNull List<LabelResponse> labels,
    @NotNull Duration duration,
    @NotNull Duration elapsed,
    @NotNull Duration minScopeDuration,
    @NotNull Duration maxScopeDuration,
    @NotNull List<ScopeResponse> scopes,
    @NotNull List<Long> dependencies,
    @NotNull List<Long> dependents
) implements TaskResponse {
    public DynamicTaskResponse(Long id, String organizationId, String name, String description,
            Task.Difficulty difficulty, Instant startAt, Instant endAt, List<LabelResponse> labels,
            Duration duration, Duration elapsed, Duration minScopeDuration, Duration maxScopeDuration,
            List<ScopeResponse> scopes, List<Long> dependencies, List<Long> dependents) {
        this(id, organizationId, null, name, description, difficulty, startAt, endAt, labels, duration, elapsed, minScopeDuration, maxScopeDuration, scopes, dependencies, dependents);
    }
}

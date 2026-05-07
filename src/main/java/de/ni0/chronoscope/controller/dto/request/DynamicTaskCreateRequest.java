package de.ni0.chronoscope.controller.dto.request;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import de.ni0.chronoscope.model.Task;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Create request for a schedulable task with duration, scope-size limits, and dependencies.
 */
@Schema(description = "Create request for a dynamic (schedulable) task with duration and scope constraints")
public record DynamicTaskCreateRequest(
    @NotBlank String organizationId,
    @NotBlank String name,
    @NotNull String description,
    @NotNull Task.Difficulty difficulty,
    @NotNull Instant startAt,
    @NotNull Instant endAt,
    @NotNull List<@Valid @NotNull LabelCreateRequest> labels,
    @NotNull Duration duration,
    @NotNull Duration minScopeDuration,
    @NotNull Duration maxScopeDuration,
    @NotNull List<@NotNull Long> dependencies
) implements TaskCreateRequest {
}

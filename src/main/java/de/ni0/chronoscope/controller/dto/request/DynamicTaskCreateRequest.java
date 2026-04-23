package de.ni0.chronoscope.controller.dto.request;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Create request for a dynamic (schedulable) task with duration and scope constraints")
public record DynamicTaskCreateRequest(
    @NotNull Long accountId,
    @NotBlank String name,
    @NotNull String description,
    @NotNull String rrule,
    @NotNull @Positive Integer difficulty,
    @NotNull Instant startAt,
    @NotNull Instant endAt,
    @NotNull List<@Valid @NotNull LabelCreateRequest> labels,
    @NotNull Duration duration,
    @NotNull Duration minScopeDuration,
    @NotNull Duration maxScopeDuration,
    @NotNull List<@Valid @NotNull TaskDependencyCreateRequest> dependencies
) implements TaskCreateRequest {
}

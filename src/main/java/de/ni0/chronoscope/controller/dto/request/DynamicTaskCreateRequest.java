package de.ni0.chronoscope.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.List;

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
    @NotNull @Positive Integer duration,
    @NotNull @Positive Integer minScopeDuration,
    @NotNull @Positive Integer maxScopeDuration
) implements TaskCreateRequest {
}

package de.ni0.chronoscope.controller.dto.request;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Create request for a dynamic (schedulable) task with duration and scope constraints")
public record DynamicTaskCreateRequest(
    @NotNull Long accountId,
    @NotBlank String name,
    @NotNull String description,
    @NotNull String rrule,
    @NotNull @Positive @Max(5) Integer difficulty,
    @NotNull Instant startAt,
    @NotNull Instant endAt,
    @NotNull List<@Valid @NotNull LabelCreateRequest> labels,
    @NotNull @JsonFormat(pattern = "PT[hours]H[minutes]M[seconds]S", shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Duration duration,
    @NotNull @JsonFormat(pattern = "PT[hours]H[minutes]M[seconds]S", shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Duration minScopeDuration,
    @NotNull @JsonFormat(pattern = "PT[hours]H[minutes]M[seconds]S", shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Duration maxScopeDuration,
    @NotNull List<@NotNull Long> dependencies
) implements TaskCreateRequest {
}

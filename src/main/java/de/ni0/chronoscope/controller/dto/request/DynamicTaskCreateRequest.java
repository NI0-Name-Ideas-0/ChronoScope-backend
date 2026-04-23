package de.ni0.chronoscope.controller.dto.request;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.time.DurationMin;

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
    @NotNull @DurationMin(nanos = 1) Duration duration,
    @NotNull @DurationMin(nanos = 1) Duration minScopeDuration,
    @NotNull @DurationMin(nanos = 1) Duration maxScopeDuration,
    @NotNull List<@Valid @NotNull TaskDependencyCreateRequest> dependencies
) implements TaskCreateRequest {

    @AssertTrue(message = "minScopeDuration must be less than or equal to maxScopeDuration")
    public boolean isScopeDurationRangeValid() {
        return minScopeDuration != null && maxScopeDuration != null && !minScopeDuration.minus(maxScopeDuration).isPositive();
    }
}

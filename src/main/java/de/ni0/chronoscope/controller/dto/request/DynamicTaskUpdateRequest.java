package de.ni0.chronoscope.controller.dto.request;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Update request for a dynamic (schedulable) task")
public record DynamicTaskUpdateRequest(
    Long organizationId,
    @Size(min = 1) String name,
    String description,
    String rrule,
    @Positive @Max(5) Integer difficulty,
    Instant startAt,
    Instant endAt,
    List<@Valid @NotNull LabelCreateRequest> labels,
    Duration duration,
    Duration elapsed,
    Duration minScopeDuration,
    Duration maxScopeDuration,
    List<@NotNull Long> dependencies
) implements TaskUpdateRequest {

    @JsonIgnore
    @AssertTrue(message = "duration must be non-negative")
    public boolean isDurationValid() {
        return duration == null || !duration.isNegative();
    }
    @JsonIgnore
    @AssertTrue(message = "elapsed must be non-negative")
    public boolean isElapsedValid() {
        return elapsed == null || !elapsed.isNegative();
    }
}

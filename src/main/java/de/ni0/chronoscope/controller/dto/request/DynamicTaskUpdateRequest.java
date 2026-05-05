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

/**
 * Partial update request for a dynamic task.
 *
 * <p>Null fields are ignored by the mapper, while provided fields are validated before the
 * service persists the update.</p>
 */
@Schema(description = "Update request for a dynamic (schedulable) task")
public record DynamicTaskUpdateRequest(
    Long accountId,
    Long organizationId,
    @Size(min = 1) String name,
    String description,
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

    /**
     * Validation hook that permits omitted duration updates while rejecting negative values.
     *
     * @return {@code true} when the duration is absent or non-negative
     */
    @JsonIgnore
    @AssertTrue(message = "duration must be non-negative")
    public boolean isDurationValid() {
        return duration == null || !duration.isNegative();
    }

    /**
     * Validation hook that permits omitted elapsed-time updates while rejecting negative values.
     *
     * @return {@code true} when elapsed time is absent or non-negative
     */
    @JsonIgnore
    @AssertTrue(message = "elapsed must be non-negative")
    public boolean isElapsedValid() {
        return elapsed == null || !elapsed.isNegative();
    }
}

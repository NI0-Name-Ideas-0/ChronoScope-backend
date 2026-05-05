package de.ni0.chronoscope.controller.dto.request;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Partial update request for a fixed-time task.
 */
@Schema(description = "Update request for a static (fixed-time) task")
public record StaticTaskUpdateRequest(
    Long accountId,
    String organizationId,
    @Size(min = 1) String name,
    String description,
    String rrule,
    @Positive @Max(5) Integer difficulty,
    Instant startAt,
    Instant endAt,
    List<@Valid @NotNull LabelCreateRequest> labels,
    Boolean isBlocker
) implements TaskUpdateRequest {
}

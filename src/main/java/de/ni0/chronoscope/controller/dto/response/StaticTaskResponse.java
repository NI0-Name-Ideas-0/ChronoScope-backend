package de.ni0.chronoscope.controller.dto.response;

import de.ni0.chronoscope.model.Task;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

/**
 * API representation of a fixed-time task.
 */
@Schema(description = "Response for a static (fixed-time) task")
public record StaticTaskResponse(
    @NotNull Long id,
    @Schema(description = "Organization ID. Null for blocker tasks that are not assigned to an organizationId.", nullable = true)
    String organizationId,
    @NotNull String name,
    String description,
    @NotNull Task.Difficulty difficulty,
    @NotNull Instant startAt,
    @NotNull Instant endAt,
    @NotNull String rrule,
    @NotNull List<LabelResponse> labels,
    @NotNull Boolean isBlocker
) implements TaskResponse {
}

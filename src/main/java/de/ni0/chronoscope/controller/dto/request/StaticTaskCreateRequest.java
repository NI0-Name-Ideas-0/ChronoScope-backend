package de.ni0.chronoscope.controller.dto.request;

import java.time.Instant;
import java.util.List;

import de.ni0.chronoscope.model.ColorToken;
import de.ni0.chronoscope.model.Task;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Create request for a fixed-time task.
 *
 * <p>Non-blocker static tasks must be assigned to an organizationId; blocker tasks may be
 * organizationId-independent.</p>
 */
@Schema(description = "Create request for a static (fixed-time) task")
@ValidStaticTaskOrganization
public record StaticTaskCreateRequest(
    @Schema(description = "Organization ID. Required unless this static task is a blocker.")
    String organizationId,
    @NotNull ColorToken color,
    @NotBlank String name,
    @NotNull String description,
    @NotNull String rrule,
    @NotNull Task.Difficulty difficulty,
    @NotNull Instant startAt,
    @NotNull Instant endAt,
    @NotNull List<@Valid @NotNull LabelCreateRequest> labels,
    @NotNull Boolean isBlocker
) implements TaskCreateRequest {
}

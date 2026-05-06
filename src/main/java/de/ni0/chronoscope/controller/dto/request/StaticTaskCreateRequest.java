package de.ni0.chronoscope.controller.dto.request;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Create request for a fixed-time task.
 *
 * <p>Non-blocker static tasks must be assigned to an organization; blocker tasks may be
 * organization-independent.</p>
 */
@Schema(description = "Create request for a static (fixed-time) task")
@ValidStaticTaskOrganization
public record StaticTaskCreateRequest(
    @Schema(description = "Organization ID. Required unless this static task is a blocker.")
    @NotBlank String organizationId,
    @NotBlank String name,
    @NotNull String description,
    @NotNull String rrule,
    @NotNull @Positive @Max(5) Integer difficulty,
    @NotNull Instant startAt,
    @NotNull Instant endAt,
    @NotNull List<@Valid @NotNull LabelCreateRequest> labels,
    @NotNull Boolean isBlocker
) implements TaskCreateRequest {
}

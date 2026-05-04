package de.ni0.chronoscope.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Response for a static (fixed-time) task")
public record StaticTaskResponse(
    Long id,
    Long accountId,
    @Schema(description = "Organization ID. Null for blocker tasks that are not assigned to an organization.", nullable = true)
    Long organizationId,
    String name,
    String description,
    Integer difficulty,
    Instant startAt,
    Instant endAt,
    String rrule,
    List<LabelResponse> labels,
    Boolean isBlocker
) implements TaskResponse {
}

package de.ni0.chronoscope.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

@Schema(description = "Create request for a static (fixed-time) task")
public record StaticTaskCreateRequest(
    @NotNull Long accountId,
    @NotBlank String name,
    String description,
    String rrule,
    Integer difficulty,
    Instant startAt,
    Instant endAt,
    List<LabelCreateRequest> labels,
    Boolean isBlocker
) implements TaskCreateRequest {
}

package de.ni0.chronoscope.controller.dto;

import java.time.Instant;
import java.util.List;

public record TaskDto(
    Long id,
    Long accountId,
    String name,
    String description,
    Integer difficulty,
    Instant startAt,
    Instant endAt,
    String rrule,
    DynamicTaskDto dynamicTask,
    StaticTaskDto staticTask,
    List<LabelDto> labels
) {
}

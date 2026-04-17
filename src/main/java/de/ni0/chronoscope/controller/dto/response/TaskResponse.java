package de.ni0.chronoscope.controller.dto.response;

import de.ni0.chronoscope.controller.dto.DynamicTaskDto;
import de.ni0.chronoscope.controller.dto.LabelDto;
import de.ni0.chronoscope.controller.dto.StaticTaskDto;

import java.time.Instant;
import java.util.List;

public record TaskResponse(
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

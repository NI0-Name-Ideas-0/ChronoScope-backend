package de.ni0.chronoscope.controller.dto.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = StaticTaskUpdateRequest.class, name = "static"),
    @JsonSubTypes.Type(value = DynamicTaskUpdateRequest.class, name = "dynamic")
})
public sealed interface TaskUpdateRequest permits StaticTaskUpdateRequest, DynamicTaskUpdateRequest {

    String name();
    String description();
    String rrule();
    Integer difficulty();
    Instant startAt();
    Instant endAt();
    List<LabelCreateRequest> labels();
}

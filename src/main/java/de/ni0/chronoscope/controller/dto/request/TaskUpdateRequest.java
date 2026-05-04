package de.ni0.chronoscope.controller.dto.request;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Polymorphic task update contract selected by the JSON {@code type} discriminator.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = StaticTaskUpdateRequest.class, name = "static"),
    @JsonSubTypes.Type(value = DynamicTaskUpdateRequest.class, name = "dynamic")
})
public sealed interface TaskUpdateRequest permits StaticTaskUpdateRequest, DynamicTaskUpdateRequest {
    Long accountId();
    Long organizationId();
    String name();
    String description();
    String rrule();
    Integer difficulty();
    Instant startAt();
    Instant endAt();
    List<LabelCreateRequest> labels();
}

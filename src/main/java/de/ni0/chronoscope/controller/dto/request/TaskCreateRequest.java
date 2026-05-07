package de.ni0.chronoscope.controller.dto.request;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.ni0.chronoscope.model.Task;

/**
 * Polymorphic task creation contract selected by the JSON {@code type} discriminator.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = StaticTaskCreateRequest.class, name = "static"),
    @JsonSubTypes.Type(value = DynamicTaskCreateRequest.class, name = "dynamic")
})
public sealed interface TaskCreateRequest permits StaticTaskCreateRequest, DynamicTaskCreateRequest {
    String organizationId();
    String name();
    String description();
    Task.Difficulty difficulty();
    Instant startAt();
    Instant endAt();
    List<LabelCreateRequest> labels();
}

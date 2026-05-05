package de.ni0.chronoscope.controller.dto.response;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Polymorphic task response contract emitted with the JSON {@code type} discriminator.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = StaticTaskResponse.class, name = "static"),
    @JsonSubTypes.Type(value = DynamicTaskResponse.class, name = "dynamic")
})
public sealed interface TaskResponse permits StaticTaskResponse, DynamicTaskResponse {

    Long id();
    Long accountId();
    Long organizationId();
    String name();
    String description();
    Integer difficulty();
    Instant startAt();
    Instant endAt();
    List<LabelResponse> labels();
}

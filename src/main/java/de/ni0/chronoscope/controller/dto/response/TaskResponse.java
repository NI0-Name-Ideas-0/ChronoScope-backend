package de.ni0.chronoscope.controller.dto.response;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotNull;

/**
 * Polymorphic task response contract emitted with the JSON {@code type} discriminator.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = StaticTaskResponse.class, name = "static"),
    @JsonSubTypes.Type(value = DynamicTaskResponse.class, name = "dynamic")
})
public sealed interface TaskResponse permits StaticTaskResponse, DynamicTaskResponse {

    @NotNull
    Long id();
    String organizationId();
    @NotNull String name();
    String description();
    @NotNull Integer difficulty();
    @NotNull Instant startAt();
    @NotNull Instant endAt();
    @NotNull List<LabelResponse> labels();
}

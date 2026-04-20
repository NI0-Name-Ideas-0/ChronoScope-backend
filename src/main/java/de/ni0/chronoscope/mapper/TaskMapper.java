package de.ni0.chronoscope.mapper;

import java.util.ArrayList;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import de.ni0.chronoscope.controller.dto.request.DynamicTaskCreateRequest;
import de.ni0.chronoscope.controller.dto.request.DynamicTaskUpdateRequest;
import de.ni0.chronoscope.controller.dto.request.StaticTaskCreateRequest;
import de.ni0.chronoscope.controller.dto.request.StaticTaskUpdateRequest;
import de.ni0.chronoscope.controller.dto.response.DynamicTaskResponse;
import de.ni0.chronoscope.controller.dto.response.StaticTaskResponse;
import de.ni0.chronoscope.model.DynamicTask;
import de.ni0.chronoscope.model.StaticTask;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    uses = {LabelMapper.class, ScopeMapper.class, TaskDependencyMapper.class},
    unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface TaskMapper {

    // --- Static task: create ---
    @Mapping(target = "id", ignore = true)
    // Account is validated and assigned by controller logic after request parsing.
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "difficulty", source = "difficulty")
    @Mapping(target = "startAt", source = "startAt")
    @Mapping(target = "endAt", source = "endAt")
    @Mapping(target = "rrule", source = "rrule")
    @Mapping(target = "labels", source = "labels")
    @Mapping(target = "isBlocker", source = "isBlocker")
    StaticTask fromCreateRequest(StaticTaskCreateRequest request);

    // --- Dynamic task: create ---
    @Mapping(target = "id", ignore = true)
    // Account is validated and assigned by controller logic after request parsing.
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "difficulty", source = "difficulty")
    @Mapping(target = "startAt", source = "startAt")
    @Mapping(target = "endAt", source = "endAt")
    @Mapping(target = "rrule", source = "rrule")
    @Mapping(target = "labels", source = "labels")
    @Mapping(target = "duration", source = "duration")
    @Mapping(target = "minScopeDuration", source = "minScopeDuration")
    @Mapping(target = "maxScopeDuration", source = "maxScopeDuration")
    @Mapping(target = "scopes", expression = "java(new java.util.ArrayList<>())") // default to empty list because it's not provided by request
    @Mapping(target = "dependencies", source = "dependencies")
    @Mapping(target = "elapsed", constant = "0") // default to 0 because it's not provided by request
    DynamicTask fromCreateRequest(DynamicTaskCreateRequest request);

    //! DO NOT USE YET
    // TODO: how will we handle task updates?
    // --- Static task: update ---
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "labels", ignore = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "difficulty", source = "difficulty")
    @Mapping(target = "startAt", source = "startAt")
    @Mapping(target = "endAt", source = "endAt")
    @Mapping(target = "rrule", source = "rrule")
    @Mapping(target = "isBlocker", source = "isBlocker")
    @Deprecated
    void fromUpdateRequest(StaticTaskUpdateRequest request, @MappingTarget StaticTask task);

    //! DO NOT USE YET
    // TODO: how will we handle task updates?
    // --- Dynamic task: update ---
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "labels", ignore = true)
    @Mapping(target = "scopes", ignore = true)
    @Mapping(target = "dependencies", ignore = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "difficulty", source = "difficulty")
    @Mapping(target = "startAt", source = "startAt")
    @Mapping(target = "endAt", source = "endAt")
    @Mapping(target = "rrule", source = "rrule")
    @Mapping(target = "duration", source = "duration")
    @Mapping(target = "elapsed", source = "elapsed")
    @Mapping(target = "minScopeDuration", source = "minScopeDuration")
    @Mapping(target = "maxScopeDuration", source = "maxScopeDuration")
    @Deprecated
    void fromUpdateRequest(DynamicTaskUpdateRequest request, @MappingTarget DynamicTask task);

    // --- Response mapping ---
    @Mapping(target = "accountId", source = "account.id")
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "difficulty", source = "difficulty")
    @Mapping(target = "startAt", source = "startAt")
    @Mapping(target = "endAt", source = "endAt")
    @Mapping(target = "rrule", source = "rrule")
    @Mapping(target = "labels", source = "labels")
    @Mapping(target = "isBlocker", source = "isBlocker")
    StaticTaskResponse toResponse(StaticTask task);

    @Mapping(target = "accountId", source = "account.id")
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "difficulty", source = "difficulty")
    @Mapping(target = "startAt", source = "startAt")
    @Mapping(target = "endAt", source = "endAt")
    @Mapping(target = "rrule", source = "rrule")
    @Mapping(target = "labels", source = "labels")
    @Mapping(target = "duration", source = "duration")
    @Mapping(target = "elapsed", source = "elapsed")
    @Mapping(target = "minScopeDuration", source = "minScopeDuration")
    @Mapping(target = "maxScopeDuration", source = "maxScopeDuration")
    @Mapping(target = "scopes", source = "scopes")
    @Mapping(target = "dependencies", source = "dependencies")
    DynamicTaskResponse toResponse(DynamicTask task);

    // --- After-mapping: wire bidirectional Label -> Task ---
    // Label.task is the owning side; Task.labels is inverse.
    // Hibernate reads the owning side for the FK, so we must set it.
    // TODO: labels Many To Many relation refactor maybe?
    @AfterMapping
    default void wireLabels(@MappingTarget StaticTask task) {
        if (task.getLabels() == null) return;
        for (var label : task.getLabels()) {
            label.setTask(task);
        }
    }

    @AfterMapping
    default void wireLabels(@MappingTarget DynamicTask task) {
        if (task.getLabels() == null) return;
        for (var label : task.getLabels()) {
            label.setTask(task);
        }
    }

    @AfterMapping
    default void wireDependencies(@MappingTarget DynamicTask task) {
        if (task.getDependencies() == null) {
            task.setDependencies(new ArrayList<>());
            return;
        }
        for (var dependency : task.getDependencies()) {
            dependency.setDynamicTask(task);
        }
    }
}
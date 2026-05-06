package de.ni0.chronoscope.mapper;

import de.ni0.chronoscope.controller.dto.request.WorkSlotCreateRequest;
import de.ni0.chronoscope.controller.dto.request.WorkSlotUpdateRequest;
import de.ni0.chronoscope.controller.dto.response.WorkSlotResponse;
import de.ni0.chronoscope.model.WorkSlot;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for work-slot requests and responses.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {AccountProxyProvider.class})
public interface WorkSlotMapper {

    /**
     * Converts a create request to a work slot with account and organization references.
     *
     * @param request create payload
     * @return new work slot entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organization", source = "organizationId")
    WorkSlot fromCreateRequest(WorkSlotCreateRequest request);

    /**
     * Applies patch fields to a work slot entity.
     *
     * @param request update payload
     * @param workSlot target work slot
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "identity", ignore = true)
    @Mapping(target = "organization", ignore = true)
    void fromUpdateRequest(WorkSlotUpdateRequest request, @MappingTarget WorkSlot workSlot);

    /**
     * Converts a work slot entity to its API representation.
     *
     * @param workSlot work slot entity
     * @return response DTO
     */
    @Mapping(target = "organizationId", source = "organization")
    WorkSlotResponse toResponse(WorkSlot workSlot);
}

package de.ni0.chronoscope.mapper;

import de.ni0.chronoscope.controller.dto.request.WorkSlotCreateRequest;
import de.ni0.chronoscope.controller.dto.request.WorkSlotUpdateRequest;
import de.ni0.chronoscope.controller.dto.response.WorkSlotResponse;
import de.ni0.chronoscope.model.WorkSlot;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {AccountProxyProvider.class, OrganizationProxyProvider.class})
public interface WorkSlotMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "account", source = "accountId", qualifiedByName = "accountProxy")
    @Mapping(target = "organization", source = "organizationId", qualifiedByName = "organizationProxy")
    WorkSlot fromCreateRequest(WorkSlotCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "organization", ignore = true)
    void fromUpdateRequest(WorkSlotUpdateRequest request, @MappingTarget WorkSlot workSlot);

    @Mapping(target = "accountId", source = "account.id")
    @Mapping(target = "organizationId", source = "organization.id")
    WorkSlotResponse toResponse(WorkSlot workSlot);
}
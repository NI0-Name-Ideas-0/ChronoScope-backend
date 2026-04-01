package de.ni0.chronoscope.controller.dto;

import de.ni0.chronoscope.model.OrganizationSlot;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateOrganizationSlotResponse {
    OrganizationSlot createdSlot;
}

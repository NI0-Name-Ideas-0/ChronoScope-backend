package de.ni0.chronoscope.controller.dto;

import de.ni0.chronoscope.model.OrganizationSlot;
import lombok.Data;

@Data
public class CreateOrganizationSlotResponse {
    OrganizationSlot createdSlot;
}

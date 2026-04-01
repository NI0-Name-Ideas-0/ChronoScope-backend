package de.ni0.chronoscope.controller;

import de.ni0.chronoscope.controller.dto.CreateOrganizationSlotRequest;
import de.ni0.chronoscope.controller.dto.CreateOrganizationSlotResponse;
import de.ni0.chronoscope.model.OrganizationSlot;
import de.ni0.chronoscope.service.OrganizationSlotService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/organizationslots")
public class OrganizationSlotController {
    private OrganizationSlotService organizationSlotService;

    @PostMapping("/")
    public CreateOrganizationSlotResponse create(@RequestBody CreateOrganizationSlotRequest request) {
        OrganizationSlot slot = new OrganizationSlot(request.getStart(), request.getDuration());
        slot = this.organizationSlotService.createOrganizationSlot(slot);
        return new  CreateOrganizationSlotResponse(slot);
    }
}

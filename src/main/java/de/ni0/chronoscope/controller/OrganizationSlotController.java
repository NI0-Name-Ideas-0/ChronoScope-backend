package de.ni0.chronoscope.controller;

import de.ni0.chronoscope.controller.dto.CreateOrganizationSlotRequest;
import de.ni0.chronoscope.controller.dto.CreateOrganizationSlotResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/organizationslots")
public class OrganizationSlotController {
    @PostMapping("/")
    public CreateOrganizationSlotResponse create(@RequestBody CreateOrganizationSlotRequest request) {

    }
}

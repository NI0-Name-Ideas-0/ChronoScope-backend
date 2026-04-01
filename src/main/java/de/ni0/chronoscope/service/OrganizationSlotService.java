package de.ni0.chronoscope.service;

import de.ni0.chronoscope.model.OrganizationSlot;
import de.ni0.chronoscope.repository.OrganizationSlotRepository;
import org.springframework.stereotype.Service;

@Service
public class OrganizationSlotService {

    private OrganizationSlotRepository organizationSlotRepository;

    public OrganizationSlot createOrganizationSlot(OrganizationSlot organizationSlot) {
        return this.organizationSlotRepository.save(organizationSlot);
    }

}

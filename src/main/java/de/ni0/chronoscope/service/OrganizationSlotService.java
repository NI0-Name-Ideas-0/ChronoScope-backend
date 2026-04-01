package de.ni0.chronoscope.service;

import de.ni0.chronoscope.model.OrganizationSlot;
import de.ni0.chronoscope.repository.OrganizationSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrganizationSlotService {

    private final OrganizationSlotRepository organizationSlotRepository;

    public OrganizationSlot createOrganizationSlot(OrganizationSlot organizationSlot) {
        return this.organizationSlotRepository.save(organizationSlot);
    }

}

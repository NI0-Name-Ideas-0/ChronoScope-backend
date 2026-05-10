package de.ni0.chronoscope.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.ni0.chronoscope.controller.dto.request.WorkSlotUpdateRequest;
import de.ni0.chronoscope.exception.InvalidRequestException;
import de.ni0.chronoscope.exception.ResourceNotFoundException;
import de.ni0.chronoscope.model.WorkSlot;
import de.ni0.chronoscope.repository.WorkSlotRepository;
import lombok.RequiredArgsConstructor;

/**
 * Manages availability windows used by the planning algorithm.
 */
@Service
@RequiredArgsConstructor
public class WorkSlotService {

    private final WorkSlotRepository workSlotRepository;

    /**
     * Returns all work slots visible to an identity through its linked accounts.
     *
     * @param identityId identity whose work slots should be loaded
     * @return matching work slots
     */
    @Transactional(readOnly = true)
    public List<WorkSlot> getWorkSlotsForIdentity(long identityId) {
        return workSlotRepository.findByIdentityId(identityId);
    }

    /**
     * Returns work slots owned by a single account.
     *
     * @param identityId identity whose slots should be loaded
     * @return matching work slots
     */
    @Transactional(readOnly = true)
    public List<WorkSlot> getWorkSlotsForIdentity(long identityId, String organizationId) {
        return workSlotRepository.findByIdentityIdAndOrganizationId(identityId, organizationId);
    }

    /**
     * Persists a new work slot.
     *
     * @param workSlot work slot to create
     * @return persisted work slot
     */
    public WorkSlot createWorkSlot(WorkSlot workSlot) {
        if (!workSlot.getStartTime().isBefore(workSlot.getEndTime())) {
            throw new InvalidRequestException("startTime must be before endTime");
        }
        return workSlotRepository.save(workSlot);
    }

    /**
     * Applies partial start/end updates through the identity boundary.
     *
     * @param identityId authenticated identity ID
     * @param id work slot ID
     * @param request patch payload
     * @return managed updated work slot
     */
    @Transactional
    public WorkSlot updateWorkSlot(long identityId, Long id, WorkSlotUpdateRequest request) {
        var workSlot = workSlotRepository.findByIdAndIdentityId(id, identityId)
                .orElseThrow(() -> new ResourceNotFoundException("Work slot not found: " + id));

        if (request.dayOfWeek() != null) workSlot.setDayOfWeek(request.dayOfWeek());
        if (request.startTime() != null) workSlot.setStartTime(request.startTime());
        if (request.endTime() != null) workSlot.setEndTime(request.endTime());

        if (!workSlot.getStartTime().isBefore(workSlot.getEndTime())) {
            throw new InvalidRequestException("startTime must be before endTime");
        }

        return workSlot;
    }

    /**
     * Deletes a work slot through the identity boundary.
     *
     * @param identityId authenticated identity ID
     * @param id work slot ID
     */
    @Transactional
    public void deleteWorkSlot(long identityId, Long id) {
        var workSlot = workSlotRepository.findByIdAndIdentityId(id, identityId)
                .orElseThrow(() -> new ResourceNotFoundException("Work slot not found: " + id));

        workSlotRepository.delete(workSlot);
    }
}

package de.ni0.chronoscope.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.ni0.chronoscope.controller.dto.request.WorkSlotUpdateRequest;
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
        return workSlotRepository.findByAccountIdentityId(identityId);
    }

    /**
     * Returns work slots owned by a single account.
     *
     * @param accountId account whose slots should be loaded
     * @return matching work slots
     */
    @Transactional(readOnly = true)
    public List<WorkSlot> getWorkSlotsForAccount(long accountId) {
        return workSlotRepository.findByAccountId(accountId);
    }

    /**
     * Persists a new work slot.
     *
     * @param workSlot work slot to create
     * @return persisted work slot
     */
    public WorkSlot createWorkSlot(WorkSlot workSlot) {
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
        var workSlot = workSlotRepository.findByIdAndAccountIdentityId(id, identityId)
                .orElseThrow(() -> new ResourceNotFoundException("Work slot not found: " + id));

        if (request.startAt() != null) workSlot.setStartAt(request.startAt());
        if (request.endAt() != null) workSlot.setEndAt(request.endAt());

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
        var workSlot = workSlotRepository.findByIdAndAccountIdentityId(id, identityId)
                .orElseThrow(() -> new ResourceNotFoundException("Work slot not found: " + id));

        workSlotRepository.delete(workSlot);
    }
}

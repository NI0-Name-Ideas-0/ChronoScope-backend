package de.ni0.chronoscope.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.ni0.chronoscope.controller.dto.request.WorkSlotUpdateRequest;
import de.ni0.chronoscope.exception.ResourceNotFoundException;
import de.ni0.chronoscope.model.WorkSlot;
import de.ni0.chronoscope.repository.WorkSlotRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkSlotService {

    private final WorkSlotRepository workSlotRepository;

    @Transactional(readOnly = true)
    public List<WorkSlot> getWorkSlotsForIdentity(long identityId) {
        return workSlotRepository.findByAccountIdentityId(identityId);
    }

    @Transactional(readOnly = true)
    public List<WorkSlot> getWorkSlotsForAccount(long accountId) {
        return workSlotRepository.findByAccountId(accountId);
    }

    public WorkSlot createWorkSlot(WorkSlot workSlot) {
        return workSlotRepository.save(workSlot);
    }

    @Transactional
    public WorkSlot updateWorkSlot(long identityId, Long id, WorkSlotUpdateRequest request) {
        var workSlot = workSlotRepository.findByIdAndAccountIdentityId(id, identityId)
                .orElseThrow(() -> new ResourceNotFoundException("Work slot not found: " + id));

        if (request.startAt() != null) workSlot.setStartAt(request.startAt());
        if (request.endAt() != null) workSlot.setEndAt(request.endAt());

        return workSlot;
    }

    @Transactional
    public void deleteWorkSlot(long identityId, Long id) {
        var workSlot = workSlotRepository.findByIdAndAccountIdentityId(id, identityId)
                .orElseThrow(() -> new ResourceNotFoundException("Work slot not found: " + id));

        workSlotRepository.delete(workSlot);
    }
}

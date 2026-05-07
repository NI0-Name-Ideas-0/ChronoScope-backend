package de.ni0.chronoscope.service;

import de.ni0.chronoscope.controller.dto.request.WorkSlotUpdateRequest;
import de.ni0.chronoscope.exception.ResourceNotFoundException;
import de.ni0.chronoscope.model.WorkSlot;
import de.ni0.chronoscope.repository.WorkSlotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkSlotServiceTest {

    @Mock
    private WorkSlotRepository workSlotRepository;

    @Test
    void getWorkSlotsForIdentity_ReturnsSlotsForIdentity() {
        WorkSlotService service = new WorkSlotService(workSlotRepository);
        long identityId = 42L;

        WorkSlot slot = new WorkSlot();
        slot.setId(1L);

        when(workSlotRepository.findByIdentityId(identityId)).thenReturn(List.of(slot));

        List<WorkSlot> result = service.getWorkSlotsForIdentity(identityId);

        assertEquals(List.of(slot), result);
        verify(workSlotRepository).findByIdentityId(identityId);
        verifyNoMoreInteractions(workSlotRepository);
    }

    @Test
    void createWorkSlot_SavesAndReturnsWorkSlot() {
        WorkSlotService service = new WorkSlotService(workSlotRepository);

        WorkSlot workSlot = new WorkSlot();
        workSlot.setStartAt(Instant.parse("2026-04-20T08:00:00Z"));
        workSlot.setEndAt(Instant.parse("2026-04-20T17:00:00Z"));

        WorkSlot savedWorkSlot = new WorkSlot();
        savedWorkSlot.setId(10L);
        savedWorkSlot.setStartAt(workSlot.getStartAt());
        savedWorkSlot.setEndAt(workSlot.getEndAt());

        when(workSlotRepository.save(any(WorkSlot.class))).thenReturn(savedWorkSlot);

        WorkSlot result = service.createWorkSlot(workSlot);

        assertEquals(savedWorkSlot, result);
        verify(workSlotRepository).save(workSlot);
        verifyNoMoreInteractions(workSlotRepository);
    }

    @Test
    void updateWorkSlot_AppliesNonNullFields() {
        WorkSlotService service = new WorkSlotService(workSlotRepository);
        long identityId = 42L;
        Long slotId = 7L;

        WorkSlot existing = new WorkSlot();
        existing.setId(slotId);
        existing.setStartAt(Instant.parse("2026-04-20T08:00:00Z"));
        existing.setEndAt(Instant.parse("2026-04-20T17:00:00Z"));

        WorkSlotUpdateRequest request = new WorkSlotUpdateRequest(
                Instant.parse("2026-04-21T09:00:00Z"),
                Instant.parse("2026-04-21T18:00:00Z")
        );

        when(workSlotRepository.findByIdAndIdentityId(slotId, identityId)).thenReturn(Optional.of(existing));

        WorkSlot result = service.updateWorkSlot(identityId, slotId, request);

        assertEquals(Instant.parse("2026-04-21T09:00:00Z"), result.getStartAt());
        assertEquals(Instant.parse("2026-04-21T18:00:00Z"), result.getEndAt());
        verify(workSlotRepository).findByIdAndIdentityId(slotId, identityId);
        verifyNoMoreInteractions(workSlotRepository);
    }

    @Test
    void updateWorkSlot_IgnoresNullFields() {
        WorkSlotService service = new WorkSlotService(workSlotRepository);
        long identityId = 42L;
        Long slotId = 7L;

        Instant originalStart = Instant.parse("2026-04-20T08:00:00Z");
        Instant originalEnd = Instant.parse("2026-04-20T17:00:00Z");

        WorkSlot existing = new WorkSlot();
        existing.setId(slotId);
        existing.setStartAt(originalStart);
        existing.setEndAt(originalEnd);

        WorkSlotUpdateRequest request = new WorkSlotUpdateRequest(null, null);

        when(workSlotRepository.findByIdAndIdentityId(slotId, identityId)).thenReturn(Optional.of(existing));

        WorkSlot result = service.updateWorkSlot(identityId, slotId, request);

        assertEquals(originalStart, result.getStartAt());
        assertEquals(originalEnd, result.getEndAt());
        verify(workSlotRepository).findByIdAndIdentityId(slotId, identityId);
        verifyNoMoreInteractions(workSlotRepository);
    }

    @Test
    void updateWorkSlot_ThrowsWhenNotFound() {
        WorkSlotService service = new WorkSlotService(workSlotRepository);
        long identityId = 42L;
        Long slotId = 99L;

        when(workSlotRepository.findByIdAndIdentityId(slotId, identityId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateWorkSlot(identityId, slotId, new WorkSlotUpdateRequest(null, null)));
        verify(workSlotRepository).findByIdAndIdentityId(slotId, identityId);
        verifyNoMoreInteractions(workSlotRepository);
    }

    @Test
    void deleteWorkSlot_DeletesWorkSlot() {
        WorkSlotService service = new WorkSlotService(workSlotRepository);
        long identityId = 42L;
        Long slotId = 5L;

        WorkSlot existing = new WorkSlot();
        existing.setId(slotId);

        when(workSlotRepository.findByIdAndIdentityId(slotId, identityId)).thenReturn(Optional.of(existing));

        service.deleteWorkSlot(identityId, slotId);

        verify(workSlotRepository).findByIdAndIdentityId(slotId, identityId);
        verify(workSlotRepository).delete(existing);
        verifyNoMoreInteractions(workSlotRepository);
    }

    @Test
    void deleteWorkSlot_ThrowsWhenNotFound() {
        WorkSlotService service = new WorkSlotService(workSlotRepository);
        long identityId = 42L;
        Long slotId = 99L;

        when(workSlotRepository.findByIdAndIdentityId(slotId, identityId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteWorkSlot(identityId, slotId));
        verify(workSlotRepository).findByIdAndIdentityId(slotId, identityId);
        verifyNoMoreInteractions(workSlotRepository);
    }
}

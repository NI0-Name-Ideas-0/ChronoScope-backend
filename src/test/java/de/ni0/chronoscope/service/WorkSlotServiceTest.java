package de.ni0.chronoscope.service;

import de.ni0.chronoscope.controller.dto.request.WorkSlotUpdateRequest;
import de.ni0.chronoscope.exception.InvalidRequestException;
import de.ni0.chronoscope.exception.ResourceNotFoundException;
import de.ni0.chronoscope.model.WorkSlot;
import de.ni0.chronoscope.repository.WorkSlotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
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
        workSlot.setDayOfWeek(DayOfWeek.MONDAY);
        workSlot.setStartTime(LocalTime.of(8, 0));
        workSlot.setEndTime(LocalTime.of(17, 0));

        WorkSlot savedWorkSlot = new WorkSlot();
        savedWorkSlot.setId(10L);
        savedWorkSlot.setDayOfWeek(workSlot.getDayOfWeek());
        savedWorkSlot.setStartTime(workSlot.getStartTime());
        savedWorkSlot.setEndTime(workSlot.getEndTime());

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
        existing.setDayOfWeek(DayOfWeek.MONDAY);
        existing.setStartTime(LocalTime.of(8, 0));
        existing.setEndTime(LocalTime.of(17, 0));

        WorkSlotUpdateRequest request = new WorkSlotUpdateRequest(
            DayOfWeek.TUESDAY,
            LocalTime.of(9, 0),
            LocalTime.of(18, 0)
        );

        when(workSlotRepository.findByIdAndIdentityId(slotId, identityId)).thenReturn(Optional.of(existing));

        WorkSlot result = service.updateWorkSlot(identityId, slotId, request);

        assertEquals(DayOfWeek.TUESDAY, result.getDayOfWeek());
        assertEquals(LocalTime.of(9, 0), result.getStartTime());
        assertEquals(LocalTime.of(18, 0), result.getEndTime());
        verify(workSlotRepository).findByIdAndIdentityId(slotId, identityId);
        verifyNoMoreInteractions(workSlotRepository);
    }

    @Test
    void updateWorkSlot_IgnoresNullFields() {
        WorkSlotService service = new WorkSlotService(workSlotRepository);
        long identityId = 42L;
        Long slotId = 7L;

        LocalTime originalStart = LocalTime.of(8, 0);
        LocalTime originalEnd = LocalTime.of(17, 0);

        WorkSlot existing = new WorkSlot();
        existing.setId(slotId);
        existing.setDayOfWeek(DayOfWeek.MONDAY);
        existing.setStartTime(originalStart);
        existing.setEndTime(originalEnd);

        WorkSlotUpdateRequest request = new WorkSlotUpdateRequest(null, null, null);

        when(workSlotRepository.findByIdAndIdentityId(slotId, identityId)).thenReturn(Optional.of(existing));

        WorkSlot result = service.updateWorkSlot(identityId, slotId, request);

        assertEquals(DayOfWeek.MONDAY, result.getDayOfWeek());
        assertEquals(originalStart, result.getStartTime());
        assertEquals(originalEnd, result.getEndTime());
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
                () -> service.updateWorkSlot(identityId, slotId, new WorkSlotUpdateRequest(null, null, null)));
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

    @Test
    void createWorkSlot_ThrowsWhenStartTimeEqualsEndTime() {
        WorkSlotService service = new WorkSlotService(workSlotRepository);

        WorkSlot workSlot = new WorkSlot();
        workSlot.setDayOfWeek(DayOfWeek.MONDAY);
        workSlot.setStartTime(LocalTime.of(8, 0));
        workSlot.setEndTime(LocalTime.of(8, 0));

        assertThrows(InvalidRequestException.class, () -> service.createWorkSlot(workSlot));
        verifyNoMoreInteractions(workSlotRepository);
    }

    @Test
    void createWorkSlot_ThrowsWhenStartTimeIsAfterEndTime() {
        WorkSlotService service = new WorkSlotService(workSlotRepository);

        WorkSlot workSlot = new WorkSlot();
        workSlot.setDayOfWeek(DayOfWeek.MONDAY);
        workSlot.setStartTime(LocalTime.of(17, 0));
        workSlot.setEndTime(LocalTime.of(8, 0));

        assertThrows(InvalidRequestException.class, () -> service.createWorkSlot(workSlot));
        verifyNoMoreInteractions(workSlotRepository);
    }

    @Test
    void updateWorkSlot_ThrowsWhenNewEndTimeIsBeforeExistingStartTime() {
        WorkSlotService service = new WorkSlotService(workSlotRepository);
        long identityId = 42L;
        Long slotId = 7L;

        WorkSlot existing = new WorkSlot();
        existing.setId(slotId);
        existing.setDayOfWeek(DayOfWeek.MONDAY);
        existing.setStartTime(LocalTime.of(10, 0));
        existing.setEndTime(LocalTime.of(17, 0));

        WorkSlotUpdateRequest request = new WorkSlotUpdateRequest(null, null, LocalTime.of(9, 0));

        when(workSlotRepository.findByIdAndIdentityId(slotId, identityId)).thenReturn(Optional.of(existing));

        assertThrows(InvalidRequestException.class, () -> service.updateWorkSlot(identityId, slotId, request));
        verify(workSlotRepository).findByIdAndIdentityId(slotId, identityId);
        verifyNoMoreInteractions(workSlotRepository);
    }

    @Test
    void updateWorkSlot_ThrowsWhenNewStartTimeIsAfterExistingEndTime() {
        WorkSlotService service = new WorkSlotService(workSlotRepository);
        long identityId = 42L;
        Long slotId = 7L;

        WorkSlot existing = new WorkSlot();
        existing.setId(slotId);
        existing.setDayOfWeek(DayOfWeek.MONDAY);
        existing.setStartTime(LocalTime.of(8, 0));
        existing.setEndTime(LocalTime.of(12, 0));

        WorkSlotUpdateRequest request = new WorkSlotUpdateRequest(null, LocalTime.of(13, 0), null);

        when(workSlotRepository.findByIdAndIdentityId(slotId, identityId)).thenReturn(Optional.of(existing));

        assertThrows(InvalidRequestException.class, () -> service.updateWorkSlot(identityId, slotId, request));
        verify(workSlotRepository).findByIdAndIdentityId(slotId, identityId);
        verifyNoMoreInteractions(workSlotRepository);
    }
}

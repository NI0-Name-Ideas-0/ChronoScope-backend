package de.ni0.chronoscope.controller;

import de.ni0.chronoscope.config.RequestContext;
import de.ni0.chronoscope.controller.dto.request.WorkSlotCreateRequest;
import de.ni0.chronoscope.controller.dto.request.WorkSlotUpdateRequest;
import de.ni0.chronoscope.controller.dto.response.WorkSlotResponse;
import de.ni0.chronoscope.exception.AccountAccessDeniedException;
import de.ni0.chronoscope.exception.AccountNotFoundException;
import de.ni0.chronoscope.exception.ResourceNotFoundException;
import de.ni0.chronoscope.mapper.WorkSlotMapper;
import de.ni0.chronoscope.model.WorkSlot;
import de.ni0.chronoscope.service.AccountService;
import de.ni0.chronoscope.service.WorkSlotService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkSlotControllerTest {

    @Mock
    private WorkSlotService workSlotService;

    @Mock
    private WorkSlotMapper workSlotMapper;

    @Mock
    private AccountService accountService;

    @Test
    void getWorkSlots_UsesCurrentIdentityAndMapsResponse() {
        RequestContext requestContext = new RequestContext();
        requestContext.setIdentityId(99L);
        WorkSlotController controller = new WorkSlotController(workSlotService, workSlotMapper, accountService, requestContext);

        WorkSlot slot = new WorkSlot();
        slot.setId(1L);

        WorkSlotResponse response = new WorkSlotResponse(1L, 10L, 20L,
                Instant.parse("2026-04-20T08:00:00Z"), Instant.parse("2026-04-20T17:00:00Z"));

        when(workSlotService.getWorkSlotsForIdentity(99L)).thenReturn(List.of(slot));
        when(workSlotMapper.toResponse(slot)).thenReturn(response);

        List<WorkSlotResponse> result = controller.getWorkSlots();

        assertEquals(List.of(response), result);
        verify(workSlotService).getWorkSlotsForIdentity(99L);
        verify(workSlotMapper).toResponse(slot);
        verifyNoMoreInteractions(workSlotService, workSlotMapper, accountService);
    }

    @Test
    void createWorkSlot_ReturnsCreatedWhenAuthorized() {
        RequestContext requestContext = new RequestContext();
        requestContext.setIdentityId(99L);
        WorkSlotController controller = new WorkSlotController(workSlotService, workSlotMapper, accountService, requestContext);

        WorkSlotCreateRequest request = new WorkSlotCreateRequest(
                10L, 20L,
                Instant.parse("2026-04-20T08:00:00Z"), Instant.parse("2026-04-20T17:00:00Z"));

        WorkSlot mappedSlot = new WorkSlot();
        WorkSlot savedSlot = new WorkSlot();
        savedSlot.setId(7L);

        WorkSlotResponse expectedResponse = new WorkSlotResponse(7L, 10L, 20L,
                Instant.parse("2026-04-20T08:00:00Z"), Instant.parse("2026-04-20T17:00:00Z"));

        when(workSlotMapper.fromCreateRequest(request)).thenReturn(mappedSlot);
        when(workSlotService.createWorkSlot(any(WorkSlot.class))).thenReturn(savedSlot);
        when(workSlotMapper.toResponse(savedSlot)).thenReturn(expectedResponse);

        ResponseEntity<WorkSlotResponse> result = controller.createWorkSlot(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(expectedResponse, result.getBody());
        verify(accountService).validateAccountOwnership(99L, 10L);
        verify(workSlotMapper).fromCreateRequest(request);
        verify(workSlotService).createWorkSlot(any(WorkSlot.class));
        verify(workSlotMapper).toResponse(savedSlot);
        verifyNoMoreInteractions(workSlotService, workSlotMapper, accountService);
    }

    @Test
    void createWorkSlot_ThrowsWhenAccountNotFound() {
        RequestContext requestContext = new RequestContext();
        requestContext.setIdentityId(99L);
        WorkSlotController controller = new WorkSlotController(workSlotService, workSlotMapper, accountService, requestContext);

        WorkSlotCreateRequest request = new WorkSlotCreateRequest(
                99L, 20L,
                Instant.parse("2026-04-20T08:00:00Z"), Instant.parse("2026-04-20T17:00:00Z"));

        doThrow(new AccountNotFoundException()).when(accountService).validateAccountOwnership(99L, 99L);

        assertThrows(AccountNotFoundException.class, () -> controller.createWorkSlot(request));
        verify(accountService).validateAccountOwnership(99L, 99L);
        verifyNoMoreInteractions(workSlotService, workSlotMapper, accountService);
    }

    @Test
    void createWorkSlot_ThrowsWhenAccountBelongsToDifferentIdentity() {
        RequestContext requestContext = new RequestContext();
        requestContext.setIdentityId(99L);
        WorkSlotController controller = new WorkSlotController(workSlotService, workSlotMapper, accountService, requestContext);

        WorkSlotCreateRequest request = new WorkSlotCreateRequest(
                55L, 20L,
                Instant.parse("2026-04-20T08:00:00Z"), Instant.parse("2026-04-20T17:00:00Z"));

        doThrow(new AccountAccessDeniedException("accountId is not linked to authenticated identity"))
                .when(accountService).validateAccountOwnership(99L, 55L);

        assertThrows(AccountAccessDeniedException.class, () -> controller.createWorkSlot(request));
        verify(accountService).validateAccountOwnership(99L, 55L);
        verifyNoMoreInteractions(workSlotService, workSlotMapper, accountService);
    }

    @Test
    void updateWorkSlot_DelegatesToServiceAndMapsResponse() {
        RequestContext requestContext = new RequestContext();
        requestContext.setIdentityId(99L);
        WorkSlotController controller = new WorkSlotController(workSlotService, workSlotMapper, accountService, requestContext);

        WorkSlotUpdateRequest request = new WorkSlotUpdateRequest(
                Instant.parse("2026-04-21T09:00:00Z"), Instant.parse("2026-04-21T18:00:00Z"));

        WorkSlot updatedSlot = new WorkSlot();
        updatedSlot.setId(7L);

        WorkSlotResponse expectedResponse = new WorkSlotResponse(7L, 10L, 20L,
                Instant.parse("2026-04-21T09:00:00Z"), Instant.parse("2026-04-21T18:00:00Z"));

        when(workSlotService.updateWorkSlot(99L, 7L, request)).thenReturn(updatedSlot);
        when(workSlotMapper.toResponse(updatedSlot)).thenReturn(expectedResponse);

        WorkSlotResponse result = controller.updateWorkSlot(7L, request);

        assertEquals(expectedResponse, result);
        verify(workSlotService).updateWorkSlot(99L, 7L, request);
        verify(workSlotMapper).toResponse(updatedSlot);
        verifyNoMoreInteractions(workSlotService, workSlotMapper, accountService);
    }

    @Test
    void updateWorkSlot_ThrowsWhenNotFound() {
        RequestContext requestContext = new RequestContext();
        requestContext.setIdentityId(99L);
        WorkSlotController controller = new WorkSlotController(workSlotService, workSlotMapper, accountService, requestContext);

        WorkSlotUpdateRequest request = new WorkSlotUpdateRequest(null, null);

        when(workSlotService.updateWorkSlot(99L, 999L, request)).thenThrow(new ResourceNotFoundException("Work slot not found: 999"));

        assertThrows(ResourceNotFoundException.class, () -> controller.updateWorkSlot(999L, request));
        verify(workSlotService).updateWorkSlot(99L, 999L, request);
        verifyNoMoreInteractions(workSlotService, workSlotMapper, accountService);
    }

    @Test
    void deleteWorkSlot_DelegatesToService() {
        RequestContext requestContext = new RequestContext();
        requestContext.setIdentityId(99L);
        WorkSlotController controller = new WorkSlotController(workSlotService, workSlotMapper, accountService, requestContext);

        doNothing().when(workSlotService).deleteWorkSlot(99L, 5L);

        controller.deleteWorkSlot(5L);

        verify(workSlotService).deleteWorkSlot(99L, 5L);
        verifyNoMoreInteractions(workSlotService, workSlotMapper, accountService);
    }

    @Test
    void deleteWorkSlot_ThrowsWhenNotFound() {
        RequestContext requestContext = new RequestContext();
        requestContext.setIdentityId(99L);
        WorkSlotController controller = new WorkSlotController(workSlotService, workSlotMapper, accountService, requestContext);

        doThrow(new ResourceNotFoundException("Work slot not found: 999")).when(workSlotService).deleteWorkSlot(99L, 999L);

        assertThrows(ResourceNotFoundException.class, () -> controller.deleteWorkSlot(999L));
        verify(workSlotService).deleteWorkSlot(99L, 999L);
        verifyNoMoreInteractions(workSlotService, workSlotMapper, accountService);
    }
}

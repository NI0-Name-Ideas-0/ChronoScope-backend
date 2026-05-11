package de.ni0.chronoscope.controller;

import de.ni0.chronoscope.TestData;
import de.ni0.chronoscope.config.RequestContext;
import de.ni0.chronoscope.controller.dto.request.WorkSlotCreateRequest;
import de.ni0.chronoscope.controller.dto.request.WorkSlotUpdateRequest;
import de.ni0.chronoscope.controller.dto.response.WorkSlotResponse;
import de.ni0.chronoscope.exception.ResourceNotFoundException;
import de.ni0.chronoscope.mapper.WorkSlotMapper;
import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.WorkSlot;
import de.ni0.chronoscope.service.AccountService;
import de.ni0.chronoscope.service.KeycloakService;
import de.ni0.chronoscope.service.WorkSlotService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

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

    private static final long IDENTITY_ID = 99L;
    private static final long ACCOUNT_ID = 100L;

    @Mock
    private WorkSlotService workSlotService;

    @Mock
    private WorkSlotMapper workSlotMapper;

    @Mock
    private AccountService accountService;

    @Mock
    private KeycloakService keycloakService;

    @Test
    void getWorkSlots_UsesCurrentIdentityAndMapsResponse() {
        Account account = TestData.account(IDENTITY_ID, ACCOUNT_ID);
        RequestContext requestContext = new RequestContext();
        requestContext.setAccount(account);
        WorkSlotController controller = new WorkSlotController(workSlotService, workSlotMapper, requestContext, keycloakService);

        WorkSlot slot = new WorkSlot();
        slot.setId(1L);

        WorkSlotResponse response = new WorkSlotResponse(1L, UUID.randomUUID().toString(),
            DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(17, 0));

        when(workSlotService.getWorkSlotsForIdentity(account.getIdentity().getId())).thenReturn(List.of(slot));
        when(workSlotMapper.toResponse(slot)).thenReturn(response);

        List<WorkSlotResponse> result = controller.getWorkSlots();

        assertEquals(List.of(response), result);
        verify(workSlotService).getWorkSlotsForIdentity(account.getIdentity().getId());
        verify(workSlotMapper).toResponse(slot);
        verifyNoMoreInteractions(workSlotService, workSlotMapper, accountService);
    }

    @Test
    void createWorkSlot_ReturnsCreatedWhenAuthorized() {
        Account account = TestData.account(IDENTITY_ID, ACCOUNT_ID);
        RequestContext requestContext = new RequestContext();
        requestContext.setAccount(account);
        WorkSlotController controller = new WorkSlotController(workSlotService, workSlotMapper, requestContext, keycloakService);

        WorkSlotCreateRequest request = new WorkSlotCreateRequest(
                UUID.randomUUID().toString(),
            DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(17, 0));

        WorkSlot mappedSlot = new WorkSlot();
        WorkSlot savedSlot = new WorkSlot();
        savedSlot.setId(7L);

        WorkSlotResponse expectedResponse = new WorkSlotResponse(7L, request.organizationId(),
            request.dayOfWeek(), request.startTime(), request.endTime());

        when(workSlotMapper.fromCreateRequest(request)).thenReturn(mappedSlot);
        when(workSlotService.createWorkSlot(any(WorkSlot.class))).thenReturn(savedSlot);
        when(workSlotMapper.toResponse(savedSlot)).thenReturn(expectedResponse);

        ResponseEntity<WorkSlotResponse> result = controller.createWorkSlot(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(expectedResponse, result.getBody());
        verify(keycloakService).validateIdentityOrgAccess(account.getIdentity(), request.organizationId());
        verify(workSlotMapper).fromCreateRequest(request);
        verify(workSlotService).createWorkSlot(any(WorkSlot.class));
        verify(workSlotMapper).toResponse(savedSlot);
        verifyNoMoreInteractions(workSlotService, workSlotMapper, accountService);
    }

    @Test
    void updateWorkSlot_DelegatesToServiceAndMapsResponse() {
        Account account = TestData.account(IDENTITY_ID, ACCOUNT_ID);
        RequestContext requestContext = new RequestContext();
        requestContext.setAccount(account);
        WorkSlotController controller = new WorkSlotController(workSlotService, workSlotMapper, requestContext, keycloakService);

        WorkSlotUpdateRequest request = new WorkSlotUpdateRequest(
            DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(18, 0));

        WorkSlot updatedSlot = new WorkSlot();
        updatedSlot.setId(7L);

        WorkSlotResponse expectedResponse = new WorkSlotResponse(7L, UUID.randomUUID().toString(),
            request.dayOfWeek(), request.startTime(), request.endTime());

        when(workSlotService.updateWorkSlot(account.getIdentity().getId(), 7L, request)).thenReturn(updatedSlot);
        when(workSlotMapper.toResponse(updatedSlot)).thenReturn(expectedResponse);

        WorkSlotResponse result = controller.updateWorkSlot(7L, request);

        assertEquals(expectedResponse, result);
        verify(workSlotService).updateWorkSlot(account.getIdentity().getId(), 7L, request);
        verify(workSlotMapper).toResponse(updatedSlot);
        verifyNoMoreInteractions(workSlotService, workSlotMapper, accountService);
    }

    @Test
    void updateWorkSlot_ThrowsWhenNotFound() {
        Account account = TestData.account(IDENTITY_ID, ACCOUNT_ID);
        RequestContext requestContext = new RequestContext();
        requestContext.setAccount(account);
        WorkSlotController controller = new WorkSlotController(workSlotService, workSlotMapper, requestContext, keycloakService);

        WorkSlotUpdateRequest request = new WorkSlotUpdateRequest(null, null, null);

        when(workSlotService.updateWorkSlot(account.getIdentity().getId(), 999L, request)).thenThrow(new ResourceNotFoundException("Work slot not found: 999"));

        assertThrows(ResourceNotFoundException.class, () -> controller.updateWorkSlot(999L, request));
        verify(workSlotService).updateWorkSlot(account.getIdentity().getId(), 999L, request);
        verifyNoMoreInteractions(workSlotService, workSlotMapper, accountService);
    }

    @Test
    void deleteWorkSlot_DelegatesToService() {
        Account account = TestData.account(IDENTITY_ID, ACCOUNT_ID);
        RequestContext requestContext = new RequestContext();
        requestContext.setAccount(account);
        WorkSlotController controller = new WorkSlotController(workSlotService, workSlotMapper, requestContext, keycloakService);

        doNothing().when(workSlotService).deleteWorkSlot(account.getIdentity().getId(), 5L);

        controller.deleteWorkSlot(5L);

        verify(workSlotService).deleteWorkSlot(account.getIdentity().getId(), 5L);
        verifyNoMoreInteractions(workSlotService, workSlotMapper, accountService);
    }

    @Test
    void deleteWorkSlot_ThrowsWhenNotFound() {
        Account account = TestData.account(IDENTITY_ID, ACCOUNT_ID);
        RequestContext requestContext = new RequestContext();
        requestContext.setAccount(account);
        WorkSlotController controller = new WorkSlotController(workSlotService, workSlotMapper, requestContext, keycloakService);

        doThrow(new ResourceNotFoundException("Work slot not found: 999"))
                .when(workSlotService).deleteWorkSlot(account.getIdentity().getId(), 999L);

        assertThrows(ResourceNotFoundException.class, () -> controller.deleteWorkSlot(999L));
        verify(workSlotService).deleteWorkSlot(account.getIdentity().getId(), 999L);
        verifyNoMoreInteractions(workSlotService, workSlotMapper, accountService);
    }
}

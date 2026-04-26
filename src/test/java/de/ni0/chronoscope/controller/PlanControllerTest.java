package de.ni0.chronoscope.controller;

import de.ni0.chronoscope.config.RequestContext;
import de.ni0.chronoscope.controller.dto.request.PlanRequest;
import de.ni0.chronoscope.controller.dto.response.ScopeResponse;
import de.ni0.chronoscope.exception.AccountAccessDeniedException;
import de.ni0.chronoscope.exception.InsufficientSlotsException;
import de.ni0.chronoscope.mapper.ScopeMapper;
import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.DynamicTask;
import de.ni0.chronoscope.model.Scope;
import de.ni0.chronoscope.service.AccountService;
import de.ni0.chronoscope.service.PlanningService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanControllerTest {

    @Mock
    private PlanningService planningService;

    @Mock
    private ScopeMapper scopeMapper;

    @Mock
    private AccountService accountService;

    @Test
    void plan_ReturnsMappedScopeResponses_WhenPlanSucceeds() {
        RequestContext requestContext = new RequestContext();
        requestContext.setIdentityId(99L);
        PlanController controller = new PlanController(planningService, scopeMapper, accountService, requestContext);

        Account account = new Account();
        account.setId(5L);

        DynamicTask task = new DynamicTask();
        task.setId(1L);

        Scope scope = new Scope(null, task, Instant.parse("2026-04-26T08:00:00Z"), Instant.parse("2026-04-26T09:00:00Z"));
        ScopeResponse response = new ScopeResponse(10L, 1L, Instant.parse("2026-04-26T08:00:00Z"), Instant.parse("2026-04-26T09:00:00Z"));

        when(accountService.validateAccountOwnership(99L, 5L)).thenReturn(account);
        when(planningService.planTasksForAccount(5L, 20L)).thenReturn(List.of(scope));
        when(scopeMapper.toResponse(scope)).thenReturn(response);

        PlanRequest request = new PlanRequest(5L, 20L);
        List<ScopeResponse> result = controller.plan(request);

        assertEquals(List.of(response), result);
        verify(accountService).validateAccountOwnership(99L, 5L);
        verify(planningService).planTasksForAccount(5L, 20L);
        verify(scopeMapper).toResponse(scope);
        verifyNoMoreInteractions(planningService, scopeMapper, accountService);
    }

    @Test
    void plan_PropagatesException_WhenAccountOwnershipValidationFails() {
        RequestContext requestContext = new RequestContext();
        requestContext.setIdentityId(99L);
        PlanController controller = new PlanController(planningService, scopeMapper, accountService, requestContext);

        doThrow(new AccountAccessDeniedException("not your account"))
                .when(accountService).validateAccountOwnership(99L, 5L);

        PlanRequest request = new PlanRequest(5L, 20L);
        assertThrows(AccountAccessDeniedException.class, () -> controller.plan(request));

        verify(accountService).validateAccountOwnership(99L, 5L);
        verifyNoMoreInteractions(planningService, scopeMapper, accountService);
    }

    @Test
    void plan_PropagatesException_WhenPlanningServiceThrows() {
        RequestContext requestContext = new RequestContext();
        requestContext.setIdentityId(99L);
        PlanController controller = new PlanController(planningService, scopeMapper, accountService, requestContext);

        Account account = new Account();
        account.setId(5L);

        when(accountService.validateAccountOwnership(99L, 5L)).thenReturn(account);
        when(planningService.planTasksForAccount(5L, 20L))
                .thenThrow(new InsufficientSlotsException("no slots"));

        PlanRequest request = new PlanRequest(5L, 20L);
        assertThrows(InsufficientSlotsException.class, () -> controller.plan(request));

        verify(accountService).validateAccountOwnership(99L, 5L);
        verify(planningService).planTasksForAccount(5L, 20L);
        verifyNoMoreInteractions(planningService, scopeMapper, accountService);
    }
}

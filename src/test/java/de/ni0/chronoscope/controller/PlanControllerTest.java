package de.ni0.chronoscope.controller;

import de.ni0.chronoscope.TestData;
import de.ni0.chronoscope.config.RequestContext;
import de.ni0.chronoscope.controller.dto.request.PlanRequest;
import de.ni0.chronoscope.controller.dto.response.ScopeResponse;
import de.ni0.chronoscope.exception.InsufficientSlotsException;
import de.ni0.chronoscope.mapper.ScopeMapper;
import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.DynamicTask;
import de.ni0.chronoscope.model.Scope;
import de.ni0.chronoscope.service.AccountService;
import de.ni0.chronoscope.service.KeycloakService;
import de.ni0.chronoscope.service.PlanningService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Mock
    private KeycloakService keycloakService;

    @Test
    void plan_ReturnsMappedScopeResponses_WhenPlanSucceeds() {
        Account account = TestData.account();
        String orgId = UUID.randomUUID().toString();
        RequestContext requestContext = new RequestContext();
        requestContext.setAccount(account);
        PlanController controller = new PlanController(planningService, scopeMapper, requestContext, keycloakService);

        DynamicTask task = new DynamicTask();
        task.setId(1L);

        Scope scope = new Scope(null, task, Instant.parse("2026-04-26T08:00:00Z"), Instant.parse("2026-04-26T09:00:00Z"));
        ScopeResponse response = new ScopeResponse(10L, 1L, Instant.parse("2026-04-26T08:00:00Z"), Instant.parse("2026-04-26T09:00:00Z"));

        when(planningService.planTasksForIdentity(account.getIdentity(), orgId)).thenReturn(List.of(scope));
        when(scopeMapper.toResponse(scope)).thenReturn(response);

        PlanRequest request = new PlanRequest(orgId);
        List<ScopeResponse> result = controller.plan(request);

        assertEquals(List.of(response), result);
        verify(planningService).planTasksForIdentity(account.getIdentity(), orgId);
        verify(scopeMapper).toResponse(scope);
        verifyNoMoreInteractions(planningService, scopeMapper, accountService);
    }

    @Test
    void plan_PropagatesException_WhenPlanningServiceThrows() {
        Account account = TestData.account();
        String orgId = UUID.randomUUID().toString();
        RequestContext requestContext = new RequestContext();
        requestContext.setAccount(account);
        PlanController controller = new PlanController(planningService, scopeMapper, requestContext, keycloakService);

        when(planningService.planTasksForIdentity(account.getIdentity(), orgId))
                .thenThrow(new InsufficientSlotsException("no slots"));

        PlanRequest request = new PlanRequest(orgId);
        assertThrows(InsufficientSlotsException.class, () -> controller.plan(request));

        verify(planningService).planTasksForIdentity(account.getIdentity(), orgId);
        verifyNoMoreInteractions(planningService, scopeMapper, accountService);
    }
}

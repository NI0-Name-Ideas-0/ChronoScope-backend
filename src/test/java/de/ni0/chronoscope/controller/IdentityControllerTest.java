package de.ni0.chronoscope.controller;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.ni0.chronoscope.TestData;
import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.service.KeycloakService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import de.ni0.chronoscope.config.RequestContext;
import de.ni0.chronoscope.controller.dto.request.AccountLinkConfirmRequest;
import de.ni0.chronoscope.controller.dto.request.AccountLinkRequest;
import de.ni0.chronoscope.controller.dto.response.IdentityResponse;
import de.ni0.chronoscope.mapper.IdentityMapper;
import de.ni0.chronoscope.model.Identity;
import de.ni0.chronoscope.service.AccountService;
import de.ni0.chronoscope.service.IdentityService;

@ExtendWith(MockitoExtension.class)
class IdentityControllerTest {

    @Mock
    private IdentityService identityService;

    @Mock
    private RequestContext requestContext;

    @Mock
    private IdentityMapper identityMapper;

    @Mock
    private KeycloakService keycloakService;

    @Test
    void getIdentity_UsesIdentityIdFromRequestContext() {
        Account account = TestData.account();
        Identity identity = account.getIdentity();
        when(requestContext.getAccount()).thenReturn(account);

        IdentityResponse expected = new IdentityResponse(identity.getId(), List.of(), List.of("dhbw-stuttgart"));

        when(identityService.getIdentity(identity.getId())).thenReturn(identity);
        when(identityMapper.toResponse(identity, List.of("dhbw-stuttgart"))).thenReturn(expected);

        IdentityController controller = new IdentityController(identityService, requestContext, identityMapper, keycloakService);

        IdentityResponse actual = controller.getIdentity();

        assertEquals(expected, actual);
        verify(requestContext).getAccount();
        verify(identityService).getIdentity(identity.getId());
        verify(identityMapper).toResponse(identity, List.of("dhbw-stuttgart"));
    }
}

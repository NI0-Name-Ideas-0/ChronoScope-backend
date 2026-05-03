package de.ni0.chronoscope.controller;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private AccountService accountService;

    @Mock
    private RequestContext requestContext;

    @Mock
    private IdentityMapper identityMapper;

    @Test
    void getIdentity_UsesIdentityIdFromRequestContext() {
        when(requestContext.getIdentityId()).thenReturn(42L);

        Identity identity = new Identity();
        identity.setId(42L);

        IdentityResponse expected = new IdentityResponse(42L, List.of());

        when(identityService.getIdentity(42L)).thenReturn(identity);
        when(identityMapper.toResponse(identity)).thenReturn(expected);

        IdentityController controller = new IdentityController(identityService, requestContext, identityMapper);

        IdentityResponse actual = controller.getIdentity();

        assertEquals(expected, actual);
        verify(requestContext).getIdentityId();
        verify(identityService).getIdentity(42L);
        verify(identityMapper).toResponse(identity);
    }

    @Test
    void requestAccountLink_ThrowsApiNotImplementedException() {
        IdentityController controller = new IdentityController(identityService, requestContext, identityMapper);

        AccountLinkRequest request = new AccountLinkRequest("target@example.com");
    }

    @Test
    void confirmAccountLink_ThrowsApiNotImplementedException() {
        IdentityController controller = new IdentityController(identityService, requestContext, identityMapper);

        AccountLinkConfirmRequest request = new AccountLinkConfirmRequest("token-value");
    }
}

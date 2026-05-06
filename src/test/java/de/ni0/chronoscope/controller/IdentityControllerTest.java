package de.ni0.chronoscope.controller;

import java.util.List;
import java.util.Set;

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
import de.ni0.chronoscope.controller.dto.response.IdentityResponse;
import de.ni0.chronoscope.mapper.IdentityMapper;
import de.ni0.chronoscope.model.Identity;
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
        Account account = TestData.account(1L, 2L);
        Identity identity = account.getIdentity();
        when(requestContext.getAccount()).thenReturn(account);
        when(identityService.getIdentity(identity.getId())).thenReturn(identity);

        IdentityResponse expected = new IdentityResponse(identity.getId(), List.of(), Set.of("dhbw-stuttgart"), Set.of());
        when(keycloakService.getAdminOrganizations(identity)).thenReturn(Set.of("dhbw-stuttgart"));
        when(identityMapper.toResponse(identity, Set.of("dhbw-stuttgart"), Set.of())).thenReturn(expected);

        IdentityController controller = new IdentityController(identityService, requestContext, identityMapper, keycloakService);

        IdentityResponse actual = controller.getIdentity();

        assertEquals(expected, actual);
        verify(requestContext).getAccount();
        verify(identityService).getIdentity(identity.getId());
        verify(keycloakService).getAdminOrganizations(identity);
        verify(identityMapper).toResponse(identity, Set.of("dhbw-stuttgart"), Set.of());
    }
}

package de.ni0.chronoscope.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.ni0.chronoscope.TestData;
import de.ni0.chronoscope.config.RequestContext;
import de.ni0.chronoscope.controller.dto.request.SettingsUpdateRequest;
import de.ni0.chronoscope.controller.dto.response.IdentityResponse;
import de.ni0.chronoscope.mapper.IdentityMapper;
import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.Identity;
import de.ni0.chronoscope.service.IdentityService;
import de.ni0.chronoscope.service.KeycloakService;

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

        IdentityResponse expected = new IdentityResponse(identity.getId(), List.of(), Set.of("dhbw-stuttgart"), Set.of(), "en_US", "light");
        when(keycloakService.getAdminOrganizations(identity)).thenReturn(Set.of("dhbw-stuttgart"));
        when(keycloakService.getIdentityOrganizations(identity)).thenReturn(Set.of());
        when(identityMapper.toResponse(identity, Set.of("dhbw-stuttgart"), Set.of(new IdentityResponse.Organization("Privat", "private")))).thenReturn(expected);

        IdentityController controller = new IdentityController(identityService, requestContext, identityMapper, keycloakService);

        IdentityResponse actual = controller.getIdentity();

        assertEquals(expected, actual);
        verify(requestContext).getAccount();
        verifyNoInteractions(identityService);
        verify(keycloakService).getAdminOrganizations(identity);
        verify(identityMapper).toResponse(identity, Set.of("dhbw-stuttgart"), Set.of(new IdentityResponse.Organization("Privat", "private")));
    }

    @Test
    void updateSettings_UpdatesLanguageAndReturnsUpdatedIdentity() {
        Account account = TestData.account(1L, 2L);
        Identity identity = account.getIdentity();
        identity.setLanguage("de_DE");
        identity.setTheme("light");

        when(requestContext.getAccount()).thenReturn(account);
        when(identityService.updateSettings(identity.getId(), new SettingsUpdateRequest(Optional.of("de_DE"), Optional.empty()))).thenReturn(identity);

        IdentityResponse expected = new IdentityResponse(identity.getId(), List.of(), Set.of("dhbw-stuttgart"), Set.of(), "de_DE", "light");
        when(keycloakService.getAdminOrganizations(identity)).thenReturn(Set.of("dhbw-stuttgart"));
        when(keycloakService.getIdentityOrganizations(identity)).thenReturn(Set.of());
        when(identityMapper.toResponse(identity, Set.of("dhbw-stuttgart"), Set.of(new IdentityResponse.Organization("Privat", "private")))).thenReturn(expected);

        IdentityController controller = new IdentityController(identityService, requestContext, identityMapper, keycloakService);
        SettingsUpdateRequest request = new SettingsUpdateRequest(Optional.of("de_DE"), Optional.empty());

        IdentityResponse actual = controller.updateSettings(request);

        assertEquals(expected, actual);
        verify(requestContext, org.mockito.Mockito.times(2)).getAccount();
        verify(identityService).updateSettings(identity.getId(), request);
        verify(keycloakService).getAdminOrganizations(identity);
        verify(keycloakService).getIdentityOrganizations(identity);
        verify(identityMapper).toResponse(identity, Set.of("dhbw-stuttgart"), Set.of(new IdentityResponse.Organization("Privat", "private")));
    }
}

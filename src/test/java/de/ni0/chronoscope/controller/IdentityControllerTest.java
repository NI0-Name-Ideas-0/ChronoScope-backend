package de.ni0.chronoscope.controller;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import de.ni0.chronoscope.config.RequestContext;
import de.ni0.chronoscope.controller.dto.request.IdentityOrganizationColorUpdateRequest;
import de.ni0.chronoscope.controller.dto.request.SettingsUpdateRequest;
import de.ni0.chronoscope.controller.dto.response.IdentityOrganizationColorResponse;
import de.ni0.chronoscope.controller.dto.response.IdentityResponse;
import de.ni0.chronoscope.controller.dto.response.SettingsResponse;
import de.ni0.chronoscope.mapper.IdentityMapper;
import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.ColorToken;
import de.ni0.chronoscope.model.Identity;
import de.ni0.chronoscope.model.IdentityOrganizationColor;
import de.ni0.chronoscope.model.WorkSettings;
import de.ni0.chronoscope.service.IdentityService;
import de.ni0.chronoscope.service.KeycloakService;

@ExtendWith(MockitoExtension.class)
class IdentityControllerTest {

    private static Account account(long identityId, long accountId) {
        Identity identity = new Identity();
        identity.setId(identityId);

        Account account = new Account();
        account.setId(accountId);
        account.setSubject(UUID.randomUUID().toString());
        account.setIdentity(identity);
        identity.setAccounts(Set.of(account));
        return account;
    }

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
        Account account = account(1L, 2L);
        Identity identity = account.getIdentity();
        when(requestContext.getAccount()).thenReturn(account);

        IdentityResponse expected = new IdentityResponse(identity.getId(), List.of(), Set.of("dhbw-stuttgart"), Set.of());
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
    void updateSettings_UpdatesLanguageAndReturnsUpdatedSettings() {
        Account account = account(1L, 2L);
        Identity identity = account.getIdentity();

        when(requestContext.getAccount()).thenReturn(account);
        var ws = new WorkSettings(480, Set.of("mo", "di", "mi", "do", "fr"));
        var settings = new de.ni0.chronoscope.model.IdentitySettings();
        settings.setLanguage("de_DE");
        settings.setTheme("light");
        settings.setWorkSettings(ws);
        when(identityService.getSettings(identity.getId())).thenReturn(settings);

        IdentityController controller = new IdentityController(identityService, requestContext, identityMapper, keycloakService);
        SettingsUpdateRequest request = new SettingsUpdateRequest(Optional.of("de_DE"), Optional.empty(), Optional.of(ws));

        SettingsResponse actual = controller.updateSettings(request);

        assertEquals("de_DE", actual.language());
        assertEquals("light", actual.theme());
        assertEquals(ws, actual.workSettings());
        verify(identityService).updateSettings(identity.getId(), request);
        verify(identityService).getSettings(identity.getId());
    }

    @Test
    void getOrganizationColors_MapsServiceRowsToResponses() {
        Account account = account(1L, 2L);
        Identity identity = account.getIdentity();
        when(requestContext.getAccount()).thenReturn(account);

        IdentityOrganizationColor color = new IdentityOrganizationColor();
        color.setOrganizationId("org-1");
        color.setColor(ColorToken.BLUE);
        when(identityService.getOrganizationColors(identity.getId())).thenReturn(List.of(color));

        IdentityController controller = new IdentityController(identityService, requestContext, identityMapper, keycloakService);

        List<IdentityOrganizationColorResponse> actual = controller.getOrganizationColors();

        assertEquals(List.of(new IdentityOrganizationColorResponse("org-1", ColorToken.BLUE)), actual);
        verify(identityService).getOrganizationColors(identity.getId());
        verifyNoInteractions(keycloakService);
    }

    @Test
    void updateOrganizationColor_ValidatesAccessAndReturnsUpdatedColor() {
        Account account = account(1L, 2L);
        Identity identity = account.getIdentity();
        when(requestContext.getAccount()).thenReturn(account);

        IdentityOrganizationColor persistedColor = new IdentityOrganizationColor();
        persistedColor.setOrganizationId("org-1");
        persistedColor.setColor(ColorToken.PURPLE);
        when(identityService.upsertOrganizationColor(identity.getId(), "org-1", ColorToken.PURPLE)).thenReturn(persistedColor);

        IdentityController controller = new IdentityController(identityService, requestContext, identityMapper, keycloakService);

        IdentityOrganizationColorResponse actual = controller.updateOrganizationColor("org-1", new IdentityOrganizationColorUpdateRequest(ColorToken.PURPLE));

        assertEquals(new IdentityOrganizationColorResponse("org-1", ColorToken.PURPLE), actual);
        verify(keycloakService).validateIdentityOrgAccess(identity, "org-1");
        verify(identityService).upsertOrganizationColor(identity.getId(), "org-1", ColorToken.PURPLE);
    }

    @Test
    void deleteOrganizationColor_ValidatesAccessAndDeletesColor() {
        Account account = account(1L, 2L);
        Identity identity = account.getIdentity();
        when(requestContext.getAccount()).thenReturn(account);

        IdentityController controller = new IdentityController(identityService, requestContext, identityMapper, keycloakService);

        controller.deleteOrganizationColor("org-1");

        verify(keycloakService).validateIdentityOrgAccess(identity, "org-1");
        verify(identityService).deleteOrganizationColor(identity.getId(), "org-1");
    }

    @Test
    void getOrganizationColor_ReturnsExistingColor() {
        Account account = account(1L, 2L);
        Identity identity = account.getIdentity();
        when(requestContext.getAccount()).thenReturn(account);

        IdentityOrganizationColor existingColor = new IdentityOrganizationColor();
        existingColor.setOrganizationId("org-1");
        existingColor.setColor(ColorToken.BLUE);
        when(identityService.getOrganizationColor(identity.getId(), "org-1")).thenReturn(existingColor);

        IdentityController controller = new IdentityController(identityService, requestContext, identityMapper, keycloakService);

        IdentityOrganizationColorResponse actual = controller.getOrganizationColor("org-1");

        assertEquals(new IdentityOrganizationColorResponse("org-1", ColorToken.BLUE), actual);
        verify(keycloakService).validateIdentityOrgAccess(identity, "org-1");
        verify(identityService).getOrganizationColor(identity.getId(), "org-1");
    }

    @Test
    void getOrganizationColor_ReturnsNewlyCreatedColorWhenNotExists() {
        Account account = account(1L, 2L);
        Identity identity = account.getIdentity();
        when(requestContext.getAccount()).thenReturn(account);

        IdentityOrganizationColor newColor = new IdentityOrganizationColor();
        newColor.setOrganizationId("org-1");
        newColor.setColor(ColorToken.PURPLE);
        when(identityService.getOrganizationColor(identity.getId(), "org-1")).thenReturn(newColor);

        IdentityController controller = new IdentityController(identityService, requestContext, identityMapper, keycloakService);

        IdentityOrganizationColorResponse actual = controller.getOrganizationColor("org-1");

        assertEquals(new IdentityOrganizationColorResponse("org-1", ColorToken.PURPLE), actual);
        verify(keycloakService).validateIdentityOrgAccess(identity, "org-1");
        verify(identityService).getOrganizationColor(identity.getId(), "org-1");
    }
}

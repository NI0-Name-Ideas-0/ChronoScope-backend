package de.ni0.chronoscope.controller;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.idm.MemberRepresentation;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import de.ni0.chronoscope.TestData;
import de.ni0.chronoscope.config.RequestContext;
import de.ni0.chronoscope.controller.dto.request.InviteUserRequest;
import de.ni0.chronoscope.controller.dto.response.OrganizationMembersResponse;
import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.service.KeycloakService;

@ExtendWith(MockitoExtension.class)
class OrganizationControllerTest {

    @Mock
    private RequestContext requestContext;

    @Mock
    private KeycloakService keycloakService;

    @Test
    void getOrganizationMembers_ValidatesAdminAccessAndMapsMembers() {
        Account account = TestData.account(42L, 7L);
        when(requestContext.getAccount()).thenReturn(account);

        MemberRepresentation member = new MemberRepresentation();
        member.setId("user-1");
        member.setUsername("jane");
        member.setFirstName("Jane");
        member.setLastName("Doe");
        member.setEmail("jane@example.com");
        when(keycloakService.getOrganizationMembers("org-1")).thenReturn(List.of(member));

        OrganizationController controller = new OrganizationController(requestContext, keycloakService);

        OrganizationMembersResponse response = controller.getOrganizationMembers("org-1");

        OrganizationMembersResponse expected = new OrganizationMembersResponse(List.of(
            new OrganizationMembersResponse.OrganizationMember(
                "user-1",
                "jane",
                "Jane",
                "Doe",
                "jane@example.com"
            )
        ));
        assertEquals(expected, response);
        verify(keycloakService).validateIdentityAdminOrgAccess(account.getIdentity(), "org-1");
        verify(keycloakService).getOrganizationMembers("org-1");
    }

    @Test
    void inviteUser_ValidatesAdminAccessAndDelegatesInvitation() {
        Account account = TestData.account(42L, 7L);
        when(requestContext.getAccount()).thenReturn(account);

        OrganizationController controller = new OrganizationController(requestContext, keycloakService);

        controller.inviteUser("org-1", new InviteUserRequest("invitee@example.com"));

        verify(keycloakService).validateIdentityAdminOrgAccess(account.getIdentity(), "org-1");
        verify(keycloakService).inviteUser("org-1", "invitee@example.com");
    }
}

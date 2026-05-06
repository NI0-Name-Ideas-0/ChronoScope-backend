package de.ni0.chronoscope.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.OrganizationsMembersResource;
import org.keycloak.admin.client.resource.OrganizationsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import de.ni0.chronoscope.exception.AccountAccessDeniedException;
import de.ni0.chronoscope.exception.AccountNotFoundException;
import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.Identity;

@ExtendWith(MockitoExtension.class)
class KeycloakServiceTest {

    @Mock
    private Keycloak keycloak;

    @Mock
    private RealmResource realmResource;

    @Mock
    private UsersResource usersResource;

    @Mock
    private UserResource userResource;

    @Mock
    private OrganizationsResource organizationsResource;

    @Mock
    private OrganizationsMembersResource organizationsMembersResource;

    private KeycloakService keycloakService;

    @BeforeEach
    void setUp() {
        keycloakService = new KeycloakService(keycloak);
        ReflectionTestUtils.setField(keycloakService, "realm", "chronoscope");
        when(keycloak.realm("chronoscope")).thenReturn(realmResource);
    }

    @Test
    void getAccountByEmail_UsesExactSearchAndThrowsWhenNoUserExists() {
        UserRepresentation user = new UserRepresentation();
        user.setId("keycloak-subject");

        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.searchByEmail("target@example.com", true)).thenReturn(List.of(user));
        when(usersResource.searchByEmail("missing@example.com", true)).thenReturn(List.of());

        assertSame(user, keycloakService.getAccountByEmail("target@example.com"));

        AccountNotFoundException exception = assertThrows(
            AccountNotFoundException.class,
            () -> keycloakService.getAccountByEmail("missing@example.com")
        );
        assertEquals("No account found for email: missing@example.com", exception.getMessage());
        verify(usersResource).searchByEmail("target@example.com", true);
        verify(usersResource).searchByEmail("missing@example.com", true);
    }

    @Test
    void validateIdentityOrgAccess_ChecksOrganizationsAcrossLinkedAccounts() {
        Identity identity = new Identity();
        account(11L, "subject-a", identity);
        account(22L, "subject-b", identity);

        when(realmResource.organizations()).thenReturn(organizationsResource);
        when(organizationsResource.members()).thenReturn(organizationsMembersResource);
        when(organizationsMembersResource.getOrganizations("subject-a")).thenReturn(List.of(organization("org-a")));
        when(organizationsMembersResource.getOrganizations("subject-b")).thenReturn(List.of(organization("org-b")));

        keycloakService.validateIdentityOrgAccess(identity, "org-b");

        AccountAccessDeniedException exception = assertThrows(
            AccountAccessDeniedException.class,
            () -> keycloakService.validateIdentityOrgAccess(identity, "org-c")
        );
        assertEquals("Account does not have access to the specified organizationId", exception.getMessage());
    }

    @Test
    void getAdminOrganizations_ReadsOrgIdSubgroupsAndValidatesAdminAccess() {
        Identity identity = new Identity();
        account(11L, "admin-subject", identity);

        GroupRepresentation adminRoot = new GroupRepresentation();
        adminRoot.setSubGroups(List.of(adminGroup("org-a"), adminGroup("org-b")));

        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get("admin-subject")).thenReturn(userResource);
        when(userResource.groups("org-admins", false)).thenReturn(List.of(adminRoot));

        assertEquals(Set.of("org-a", "org-b"), keycloakService.getAdminOrganizations(identity));
        keycloakService.validateIdentityAdminOrgAccess(identity, "org-b");

        AccountAccessDeniedException exception = assertThrows(
            AccountAccessDeniedException.class,
            () -> keycloakService.validateIdentityAdminOrgAccess(identity, "org-c")
        );
        assertEquals("No access to organizationId", exception.getMessage());
    }

    private Account account(Long id, String subject, Identity identity) {
        Account account = new Account();
        account.setId(id);
        account.setSubject(subject);
        account.setIdentity(identity);
        identity.getAccounts().add(account);
        return account;
    }

    private OrganizationRepresentation organization(String id) {
        OrganizationRepresentation organization = new OrganizationRepresentation();
        organization.setId(id);
        return organization;
    }

    private GroupRepresentation adminGroup(String organizationId) {
        GroupRepresentation group = new GroupRepresentation();
        group.setAttributes(Map.of("org-id", List.of(organizationId)));
        return group;
    }
}

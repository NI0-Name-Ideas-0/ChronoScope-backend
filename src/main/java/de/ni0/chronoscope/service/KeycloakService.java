package de.ni0.chronoscope.service;

import de.ni0.chronoscope.exception.AccountAccessDeniedException;
import de.ni0.chronoscope.exception.AccountNotFoundException;
import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.Identity;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.OrganizationMembersResource;
import org.keycloak.representations.idm.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class KeycloakService {
    private final Keycloak client;

    @Value("${keycloak.realm}")
    private String realm;

    public List<MemberRepresentation> getOrganizationMembers(String organizationId) {
        OrganizationMembersResource membersRes = client.realm(realm).organizations()
                .get(organizationId).members();
        return membersRes.list(0, 10000);
    }

    private Set<OrganizationRepresentation> getAccountOrganizations(Account account) {
        List<OrganizationRepresentation> organizations = this.client.realm(realm).organizations().members().getOrganizations(account.getSubject());
        return new HashSet<>(organizations);
    }

    public Set<OrganizationRepresentation> getIdentityOrganizations(Identity identity) {
        Set<OrganizationRepresentation> organizations = new HashSet<>();
        for (Account account : identity.getAccounts()) {
            organizations.addAll(this.getAccountOrganizations(account));
        }
        return organizations;
    }

    public UserRepresentation getAccountByEmail(String email) {
        List<UserRepresentation> results = this.client.realm(realm).users().searchByEmail(email, true);
        if (results.isEmpty()) {
            throw new AccountNotFoundException("No account found for email: " + email);
        }
        return results.getFirst();
    }

    /**
     * Verifies that the identity is linked to the requested organizationId.
     *
     * @param identity identity to validate
     * @param organizationId organizationId that must be accessible
     */
    public void validateIdentityOrgAccess(Identity identity, String organizationId) {
        if (Objects.equals(organizationId, "private")) return;
        Set<OrganizationRepresentation> organizations = this.getIdentityOrganizations(identity);
        if (organizations.stream().noneMatch(o -> Objects.equals(o.getId(), organizationId))) {
            throw new AccountAccessDeniedException("Account does not have access to the specified organizationId");
        }
    }

    public void validateIdentityAdminOrgAccess(Identity identity, String organizationId) {
        if (!this.getAdminOrganizations(identity).contains(organizationId)) {
            throw new AccountAccessDeniedException("No access to organizationId");
        }
    }

    public Set<String> getAdminOrganizations(Identity identity) {
        Set<String> organizations = new HashSet<>();
        for (Account account : identity.getAccounts()) {
            String subject = account.getSubject();
            List<GroupRepresentation> group = this.client.realm(realm).users().get(subject).groups("org-admins", false);
            if (group.isEmpty()) continue;
            GroupRepresentation first = group.getFirst();
            if (first.getSubGroups() == null) continue;
            for (GroupRepresentation subGroup : first.getSubGroups()) {
                if (subGroup == null || subGroup.getAttributes() == null) continue;
                List<String> orgIds = subGroup.getAttributes().get("org-id");
                if (orgIds == null || orgIds.isEmpty()) continue;
                String orgId = orgIds.getFirst();
                if (orgId == null || orgId.isBlank()) continue;
                organizations.add(orgId);
            }
        }
        return organizations;
    }

    public void inviteUser(String organizationId, String mail) {
        this.client.realm(realm).organizations().get(organizationId)
                .members().inviteUser(mail, "", "");
    }

    public List<OrganizationInvitationRepresentation> getInvitations(String organizationId) {
        return this.client.realm(realm).organizations().get(organizationId)
                .invitations().list();
    }

    public void deleteInvitation(String organizationId, String id) {
        this.client.realm(realm).organizations().get(organizationId)
                .invitations().delete(id);
    }

    public void resendInvitation(String organizationId, String id) {
        this.client.realm(realm).organizations().get(organizationId)
                .invitations().resend(id);
    }

    public void removeMemberFromOrganization(String organizationId, String id) {
        this.client.realm(realm).organizations().get(organizationId).members().removeMember(id);
    }
}

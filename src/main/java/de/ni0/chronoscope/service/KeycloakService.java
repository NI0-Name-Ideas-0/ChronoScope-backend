package de.ni0.chronoscope.service;

import de.ni0.chronoscope.exception.AccountAccessDeniedException;
import de.ni0.chronoscope.model.Account;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.OrganizationMembersResource;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

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

    public List<OrganizationRepresentation> getAccountOrganizations(String subject) {
        return this.client.realm(realm).organizations().members().getOrganizations(subject);
    }

    public UserRepresentation getAccountByEmail(String email) {
        return this.client.realm(realm).users().searchByEmail(email, true).getFirst();
    }

    public UserRepresentation getAccount(String subject) {
        return this.client.realm(realm).users().get(subject).toRepresentation();
    }

    /**
     * Verifies that the account is linked to the requested organization.
     *
     * @param account account to validate
     * @param organizationId organization that must be accessible
     */
    public void validateAccountOrgAccess(Account account, String organizationId) {
        List<OrganizationRepresentation> organizations = this.getAccountOrganizations(account.getSubject());
        if (organizations.stream().noneMatch(o -> Objects.equals(o.getId(), organizationId))) {
            throw new AccountAccessDeniedException("Account does not have access to the specified organization");
        }
    }
}

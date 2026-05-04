package de.ni0.chronoscope.service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import de.ni0.chronoscope.exception.AccountAccessDeniedException;
import de.ni0.chronoscope.exception.AccountNotFoundException;
import de.ni0.chronoscope.exception.InvalidRequestException;
import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.Identity;
import de.ni0.chronoscope.model.Organization;
import de.ni0.chronoscope.repository.AccountRepository;
import de.ni0.chronoscope.repository.IdentityRepository;
import de.ni0.chronoscope.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;

/**
 * Coordinates account ownership, organization access, and account synchronization from JWT data.
 */
@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final IdentityRepository identityRepository;
    private final OrganizationRepository organizationRepository;

    /**
     * Loads an account and verifies that it belongs to the authenticated identity.
     *
     * @param identityId authenticated identity ID
     * @param accountId account to validate
     * @return the validated account
     */
    public Account validateAccountOwnership(long identityId, Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(AccountNotFoundException::new);
        if (!Objects.equals(account.getIdentity().getId(), identityId)) {
            throw new AccountAccessDeniedException("accountId is not linked to authenticated identity");
        }
        return account;
    }

    /**
     * Verifies that the account is linked to the requested organization.
     *
     * @param accountId account to validate
     * @param organizationId organization that must be accessible
     */
    public void validateAccountOrgAccess(long accountId, long organizationId) {
        if (!accountRepository.existsByIdAndOrganizationsId(accountId, organizationId)) {
            throw new AccountAccessDeniedException("Account does not have access to the specified organization");
        }
    }

    /**
     * Loads an organization after confirming the account is allowed to use it.
     *
     * @param accountId account requesting the organization
     * @param organizationId organization to resolve
     * @return the resolved organization
     */
    public Organization resolveOrganizationForAccount(long accountId, long organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new InvalidRequestException("Organization not found: " + organizationId));
        validateAccountOrgAccess(accountId, organizationId);
        return organization;
    }

    /**
     * Creates or updates the account represented by an authentication token.
     *
     * <p>The method also reconciles the account's organization memberships with the token
     * claims, creating missing organizations by name.</p>
     *
     * @param subject unique external authentication subject
     * @param mail account e-mail claim
     * @param organizationNames organization names from the token
     * @return ID of the synchronized account
     */
    public long syncAccount(String subject, String mail, List<String> organizationNames) {
        // Find or create account by subject
        Account account = findOrCreateAccount(subject, mail);

        // Sync organizations
        Set<Organization> organizations = new HashSet<>(organizationNames.stream()
            .map(this::findOrCreateOrganization)
            .toList());

        account.setOrganizations(organizations);
        accountRepository.save(account);
        return account.getId();
    }

    private Account findOrCreateAccount(String subject, String mail) {
        return accountRepository.findBySubject(subject)
            .orElseGet(() -> {
                Identity identity = identityRepository.save(new Identity());
                try {
                    Account newAccount = new Account();
                    newAccount.setSubject(subject);
                    newAccount.setIdentity(identity);
                    newAccount.setMail(mail);
                    return accountRepository.save(newAccount);
                } catch (DataIntegrityViolationException ex) {
                    // Another transaction likely inserted the same unique subject concurrently.
                    try {
                        identityRepository.delete(identity);
                    } catch (Exception cleanupEx) {
                        // Best-effort cleanup; safe to continue with re-read.
                    }
                    return accountRepository.findBySubject(subject)
                        .orElseThrow(() -> new IllegalStateException("Account create raced but record was not found: " + subject, ex));
                }
            });
    }

    private Organization findOrCreateOrganization(String name) {
        return organizationRepository.findByName(name)
            .orElseGet(() -> {
                try {
                    Organization newOrg = new Organization();
                    newOrg.setName(name);
                    return organizationRepository.save(newOrg);
                } catch (DataIntegrityViolationException ex) {
                    // Another transaction likely inserted the same unique name concurrently.
                    return organizationRepository.findByName(name)
                        .orElseThrow(() -> new IllegalStateException("Organization create raced but record was not found: " + name, ex));
                }
            });
    }

}

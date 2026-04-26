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

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final IdentityRepository identityRepository;
    private final OrganizationRepository organizationRepository;

    public Account validateAccountOwnership(long identityId, Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(AccountNotFoundException::new);
        if (!Objects.equals(account.getIdentity().getId(), identityId)) {
            throw new AccountAccessDeniedException("accountId is not linked to authenticated identity");
        }
        return account;
    }

    public void validateAccountOrgAccess(long accountId, long organizationId) {
        if (!accountRepository.existsByIdAndOrganizationsId(accountId, organizationId)) {
            throw new AccountAccessDeniedException("Account does not have access to the specified organization");
        }
    }

    public Organization resolveOrganizationForAccount(long accountId, long organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new InvalidRequestException("Organization not found: " + organizationId));
        validateAccountOrgAccess(accountId, organizationId);
        return organization;
    }

    public long syncAccount(String subject, List<String> organizationNames) {
        // Find or create account by subject
        Account account = findOrCreateAccount(subject);

        // Sync organizations
        Set<Organization> organizations = new HashSet<>(organizationNames.stream()
            .map(this::findOrCreateOrganization)
            .toList());

        account.setOrganizations(organizations);
        accountRepository.save(account);
        return account.getId();
    }

    private Account findOrCreateAccount(String subject) {
        return accountRepository.findBySubject(subject)
            .orElseGet(() -> {
                Identity identity = identityRepository.save(new Identity());
                try {
                    Account newAccount = new Account();
                    newAccount.setSubject(subject);
                    newAccount.setIdentity(identity);
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

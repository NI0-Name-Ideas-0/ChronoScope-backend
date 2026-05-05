package de.ni0.chronoscope.service;

import java.util.List;
import java.util.Objects;

import org.keycloak.representations.idm.OrganizationRepresentation;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import de.ni0.chronoscope.exception.AccountAccessDeniedException;
import de.ni0.chronoscope.exception.AccountNotFoundException;
import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.Identity;
import de.ni0.chronoscope.repository.AccountRepository;
import de.ni0.chronoscope.repository.IdentityRepository;
import lombok.RequiredArgsConstructor;

/**
 * Coordinates account ownership, organization access, and account synchronization from JWT data.
 */
@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final IdentityRepository identityRepository;

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
     * Creates or updates the account represented by an authentication token.
     *
     * <p>The method also reconciles the account's organization memberships with the token
     * claims, creating missing organizations by name.</p>
     *
     * @param subject unique external authentication subject
     * @return ID of the synchronized account
     */
    public Account syncAccount(String subject) {
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
}

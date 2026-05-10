package de.ni0.chronoscope.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.Identity;
import de.ni0.chronoscope.repository.AccountRepository;
import de.ni0.chronoscope.repository.IdentityRepository;
import lombok.RequiredArgsConstructor;

/**
 * Coordinates account ownership, organizationId access, and account synchronization from JWT data.
 */
@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final IdentityRepository identityRepository;

    /**
     * Creates or updates the account represented by an authentication token.
     *
     * <p>The method also reconciles the account's organizationId memberships with the token
     * claims, creating missing organizations by name.</p>
     *
     * @param subject unique external authentication subject
     * @return ID of the synchronized account
     */
    public Account syncAccount(String subject) {
        Account account = accountRepository.findBySubject(subject)
                .orElseGet(() -> {
                    Identity identity = identityRepository.save(new Identity());
                    try {
                        Account newAccount = new Account();
                        newAccount.setSubject(subject);
                        newAccount.setIdentity(identity);
                        Account savedAccount = accountRepository.save(newAccount);
                        identity.getAccounts().add(savedAccount);
                        return savedAccount;
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
        account.getIdentity().getAccounts();
        return account;
    }
}

package de.ni0.chronoscope.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.ni0.chronoscope.exception.ResourceNotFoundException;
import de.ni0.chronoscope.model.Identity;
import de.ni0.chronoscope.repository.AccountRepository;
import de.ni0.chronoscope.repository.IdentityRepository;
import lombok.RequiredArgsConstructor;

/**
 * Service layer for identity operations.
 */
@Service
@RequiredArgsConstructor
public class IdentityService {
    private final AccountRepository accountRepository;
    private final IdentityRepository identityRepository;

    /**
     * Synchronizes the identity for the user identified by the given subject.
     *
     * @param subject the external subject from the authentication token
     * @return the resolved or newly created identity ID
     * @throws IllegalStateException if the account for the subject cannot be found
     */
    @Transactional
    public long syncIdentity(String subject) {
        return this.accountRepository.findBySubject(subject)
            .map(account -> {
                if (account.getIdentity() != null) {
                    return account.getIdentity().getId();
                }

                Identity identity = this.identityRepository.save(new Identity());
                account.setIdentity(identity);
                this.accountRepository.save(account);

                return identity.getId();
            })
            .orElseThrow(() -> new IllegalStateException("Account not found for subject: " + subject));
    }

    /**
     * Returns the identity with its accounts and organization graph.
     *
     * @param identityId the identity ID
     * @return the loaded identity
     * @throws ResourceNotFoundException if no identity exists for the given ID
     */
    @Transactional(readOnly = true)
    public Identity getIdentity(long identityId) {
        return this.identityRepository.findByIdWithAccountsAndOrganizations(identityId)
            .orElseThrow(() -> new ResourceNotFoundException("Identity not found: " + identityId));
    }
}

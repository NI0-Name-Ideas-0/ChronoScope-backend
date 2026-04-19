package de.ni0.chronoscope.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.ni0.chronoscope.model.Identity;
import de.ni0.chronoscope.repository.AccountRepository;
import de.ni0.chronoscope.repository.IdentityRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IdentityService {
    private final AccountRepository accountRepository;
    private final IdentityRepository identityRepository;

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

    @Transactional(readOnly = true)
    public Identity getIdentity(long identityId) {
        Identity identity = this.identityRepository.findById(identityId)
            .orElseThrow(() -> new IllegalStateException("Identity not found: " + identityId));
        if (identity.getAccounts() != null) {
            identity.getAccounts().size();
        }
        return identity;
    }
}

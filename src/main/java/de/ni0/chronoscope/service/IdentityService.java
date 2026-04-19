package de.ni0.chronoscope.service;

import java.util.List;

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
                identity.setAccounts(List.of(account));

                return identity.getId();
            })
            .orElse(0L);
    }
}

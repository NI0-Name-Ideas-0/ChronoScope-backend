package de.ni0.chronoscope.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.ni0.chronoscope.TestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.Identity;
import de.ni0.chronoscope.repository.AccountRepository;
import de.ni0.chronoscope.repository.IdentityRepository;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private IdentityRepository identityRepository;

    @Test
    void syncAccount_CreatesMissingAccountAndOrganizations() {
        AccountService accountService = new AccountService(accountRepository, identityRepository);

        when(accountRepository.findBySubject("subject-123")).thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account savedAccount = invocation.getArgument(0);
            savedAccount.setId(1L);
            return savedAccount;
        });
        when(identityRepository.save(any(Identity.class))).thenAnswer(invocation -> {
            Identity savedIdentity = invocation.getArgument(0);
            savedIdentity.setId(2L);
            return savedIdentity;
        });

        de.ni0.chronoscope.model.Account account = accountService.syncAccount("subject-123");

        assertEquals(1L, account.getId());
        assertEquals(2L, account.getIdentity().getId());
        assertTrue(account.getIdentity().getAccounts().contains(account));
        verify(accountRepository).findBySubject("subject-123");
        verify(accountRepository).save(any(Account.class));
        verify(identityRepository).save(any(Identity.class));
        verifyNoMoreInteractions(accountRepository, identityRepository);
    }

    @Test
    void syncAccount_ReusesExistingAccountAndOrganization() {
        AccountService accountService = new AccountService(accountRepository, identityRepository);

        Account account = TestData.account(2L, 1L);

        when(accountRepository.findBySubject(account.getSubject())).thenReturn(Optional.of(account));

        de.ni0.chronoscope.model.Account newAccount = accountService.syncAccount(account.getSubject());

        assertEquals(account.getId(), newAccount.getId());
        verify(accountRepository, times(1)).findBySubject(account.getSubject());
        verifyNoMoreInteractions(accountRepository, identityRepository);
    }
}

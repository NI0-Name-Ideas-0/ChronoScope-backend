package de.ni0.chronoscope.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
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
class IdentityServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private IdentityRepository identityRepository;

    @Test
    void syncIdentity_ReusesExistingIdentityForSameAccount() {
        IdentityService identityService = new IdentityService(accountRepository, identityRepository);

        Account account = new Account();
        account.setSubject("subject-123");

        when(accountRepository.findBySubject("subject-123")).thenReturn(Optional.of(account));
        when(identityRepository.save(any(Identity.class))).thenAnswer(invocation -> {
            Identity savedIdentity = invocation.getArgument(0);
            savedIdentity.setId(6L);
            return savedIdentity;
        });

        long firstIdentityId = identityService.syncIdentity("subject-123");
        long secondIdentityId = identityService.syncIdentity("subject-123");

        assertEquals(6L, firstIdentityId);
        assertEquals(6L, secondIdentityId);
        verify(accountRepository, times(2)).findBySubject("subject-123");
        verify(identityRepository).save(any(Identity.class));
        verify(accountRepository).save(account);
        verifyNoMoreInteractions(identityRepository, accountRepository);
    }

    @Test
    void syncIdentity_ThrowsWhenAccountIsMissing() {
        IdentityService identityService = new IdentityService(accountRepository, identityRepository);

        when(accountRepository.findBySubject("unknown-subject")).thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> identityService.syncIdentity("unknown-subject"));

        assertEquals("Account not found for subject: unknown-subject", exception.getMessage());
        verify(accountRepository).findBySubject("unknown-subject");
        verify(identityRepository, never()).save(any(Identity.class));
        verify(accountRepository, never()).save(any(Account.class));
        verifyNoMoreInteractions(accountRepository, identityRepository);
    }
}
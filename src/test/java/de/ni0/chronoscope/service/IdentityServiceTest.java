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

import de.ni0.chronoscope.exception.ResourceNotFoundException;
import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.Identity;
import de.ni0.chronoscope.repository.AccountRepository;
import de.ni0.chronoscope.repository.IdentityRepository;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class IdentityServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private IdentityRepository identityRepository;

    @Mock
    private JavaMailSender mailSender;

    @Test
    void syncIdentity_ReusesExistingIdentityForSameAccount() {
        IdentityService identityService = new IdentityService(accountRepository, identityRepository, mailSender);

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
        IdentityService identityService = new IdentityService(accountRepository, identityRepository, mailSender);

        when(accountRepository.findBySubject("unknown-subject")).thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> identityService.syncIdentity("unknown-subject"));

        assertEquals("Account not found for subject: unknown-subject", exception.getMessage());
        verify(accountRepository).findBySubject("unknown-subject");
        verify(identityRepository, never()).save(any(Identity.class));
        verify(accountRepository, never()).save(any(Account.class));
        verifyNoMoreInteractions(accountRepository, identityRepository);
    }

    @Test
    void getIdentity_ReturnsIdentityForExistingId() {
        IdentityService identityService = new IdentityService(accountRepository, identityRepository, mailSender);

        Identity identity = new Identity();
        identity.setId(99L);
        when(identityRepository.findByIdWithAccountsAndOrganizations(99L)).thenReturn(Optional.of(identity));

        Identity result = identityService.getIdentity(99L);

        assertEquals(identity, result);
        verify(identityRepository).findByIdWithAccountsAndOrganizations(99L);
        verifyNoMoreInteractions(accountRepository, identityRepository);
    }

    @Test
    void getIdentity_ThrowsWhenIdentityIsMissing() {
        IdentityService identityService = new IdentityService(accountRepository, identityRepository, mailSender);

        when(identityRepository.findByIdWithAccountsAndOrganizations(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
            () -> identityService.getIdentity(99L));

        assertEquals("Identity not found: 99", exception.getMessage());
        verify(identityRepository).findByIdWithAccountsAndOrganizations(99L);
        verifyNoMoreInteractions(accountRepository, identityRepository);
    }
}
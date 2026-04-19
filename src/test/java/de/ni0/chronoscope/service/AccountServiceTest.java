package de.ni0.chronoscope.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.Organization;
import de.ni0.chronoscope.repository.AccountRepository;
import de.ni0.chronoscope.repository.OrganizationRepository;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Test
    void syncAccount_CreatesMissingAccountAndOrganizations() {
        AccountService accountService = new AccountService(accountRepository, organizationRepository);

        when(accountRepository.findBySubject("subject-123")).thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account savedAccount = invocation.getArgument(0);
            savedAccount.setId(1L);
            return savedAccount;
        });
        when(organizationRepository.findByName("private")).thenReturn(Optional.empty());
        when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> {
            Organization savedOrganization = invocation.getArgument(0);
            savedOrganization.setId(10L);
            return savedOrganization;
        });

        long accountId = accountService.syncAccount("subject-123", List.of("private"));

        assertEquals(1L, accountId);
        verify(accountRepository).findBySubject("subject-123");
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(organizationRepository).findByName("private");
        verify(organizationRepository).save(any(Organization.class));
        verifyNoMoreInteractions(accountRepository, organizationRepository);
    }

    @Test
    void syncAccount_ReusesExistingAccountAndOrganization() {
        AccountService accountService = new AccountService(accountRepository, organizationRepository);

        Account account = new Account();
        account.setId(3L);
        account.setSubject("subject-123");

        Organization organization = new Organization();
        organization.setId(7L);
        organization.setName("private");

        when(accountRepository.findBySubject("subject-123")).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(organizationRepository.findByName("private")).thenReturn(Optional.of(organization));

        long accountId = accountService.syncAccount("subject-123", List.of("private"));

        assertEquals(3L, accountId);
        verify(accountRepository, times(1)).findBySubject("subject-123");
        verify(accountRepository).save(account);
        verify(organizationRepository).findByName("private");
        verifyNoMoreInteractions(accountRepository, organizationRepository);
    }
}
package de.ni0.chronoscope.service;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import de.ni0.chronoscope.exception.AccountAccessDeniedException;
import de.ni0.chronoscope.exception.AccountNotFoundException;
import de.ni0.chronoscope.exception.InvalidRequestException;
import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.Identity;
import de.ni0.chronoscope.model.Organization;
import de.ni0.chronoscope.repository.AccountRepository;
import de.ni0.chronoscope.repository.IdentityRepository;
import de.ni0.chronoscope.repository.OrganizationRepository;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private IdentityRepository identityRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Test
    void syncAccount_CreatesMissingAccountAndOrganizations() {
        AccountService accountService = new AccountService(accountRepository, identityRepository, organizationRepository);

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
        verify(identityRepository).save(any(Identity.class));
        verify(organizationRepository).findByName("private");
        verify(organizationRepository).save(any(Organization.class));
        verifyNoMoreInteractions(accountRepository, identityRepository, organizationRepository);
    }

    @Test
    void syncAccount_ReusesExistingAccountAndOrganization() {
        AccountService accountService = new AccountService(accountRepository, identityRepository, organizationRepository);

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
        verifyNoMoreInteractions(accountRepository, identityRepository, organizationRepository);
    }

    @Test
    void syncAccount_ReReadsOrganizationWhenConcurrentCreateViolatesUniqueConstraint() {
        AccountService accountService = new AccountService(accountRepository, identityRepository, organizationRepository);

        Account account = new Account();
        account.setId(3L);
        account.setSubject("subject-123");

        Organization organization = new Organization();
        organization.setId(7L);
        organization.setName("private");

        when(accountRepository.findBySubject("subject-123")).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(organizationRepository.findByName("private"))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(organization));
        when(organizationRepository.save(any(Organization.class)))
            .thenThrow(new DataIntegrityViolationException("duplicate key"));

        long accountId = accountService.syncAccount("subject-123", List.of("private"));

        assertEquals(3L, accountId);
        verify(accountRepository).findBySubject("subject-123");
        verify(accountRepository).save(account);
        verify(organizationRepository, times(2)).findByName("private");
        verify(organizationRepository).save(any(Organization.class));
        verifyNoMoreInteractions(accountRepository, identityRepository, organizationRepository);
    }

    @Test
    void syncAccount_ReReadsAccountWhenConcurrentCreateViolatesUniqueConstraint() {
        AccountService accountService = new AccountService(accountRepository, identityRepository, organizationRepository);

        Account account = new Account();
        account.setId(3L);
        account.setSubject("subject-123");

        Organization organization = new Organization();
        organization.setId(7L);
        organization.setName("private");

        when(accountRepository.findBySubject("subject-123"))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(account));
        when(identityRepository.save(any(Identity.class))).thenAnswer(invocation -> {
            Identity savedIdentity = invocation.getArgument(0);
            savedIdentity.setId(2L);
            return savedIdentity;
        });
        doNothing().when(identityRepository).delete(any(Identity.class));
        when(accountRepository.save(any(Account.class)))
            .thenThrow(new DataIntegrityViolationException("duplicate key"))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(organizationRepository.findByName("private")).thenReturn(Optional.of(organization));

        long accountId = accountService.syncAccount("subject-123", List.of("private"));

        assertEquals(3L, accountId);
        verify(accountRepository, times(2)).findBySubject("subject-123");
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(identityRepository).save(any(Identity.class));
        verify(identityRepository).delete(any(Identity.class));
        verify(organizationRepository).findByName("private");
        verifyNoMoreInteractions(accountRepository, identityRepository, organizationRepository);
    }

    @Test
    void validateAccountOwnership_ReturnsAccountWhenOwned() {
        AccountService accountService = new AccountService(accountRepository, identityRepository, organizationRepository);

        Identity identity = new Identity();
        identity.setId(10L);

        Account account = new Account();
        account.setId(42L);
        account.setIdentity(identity);

        when(accountRepository.findById(42L)).thenReturn(Optional.of(account));

        Account result = accountService.validateAccountOwnership(10L, 42L);

        assertEquals(account, result);
        verify(accountRepository).findById(42L);
        verifyNoMoreInteractions(accountRepository, identityRepository, organizationRepository);
    }

    @Test
    void validateAccountOwnership_ThrowsWhenAccountNotFound() {
        AccountService accountService = new AccountService(accountRepository, identityRepository, organizationRepository);

        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> accountService.validateAccountOwnership(10L, 99L));
        verify(accountRepository).findById(99L);
        verifyNoMoreInteractions(accountRepository, identityRepository, organizationRepository);
    }

    @Test
    void validateAccountOwnership_ThrowsWhenAccountBelongsToDifferentIdentity() {
        AccountService accountService = new AccountService(accountRepository, identityRepository, organizationRepository);

        Identity otherIdentity = new Identity();
        otherIdentity.setId(999L);

        Account account = new Account();
        account.setId(42L);
        account.setIdentity(otherIdentity);

        when(accountRepository.findById(42L)).thenReturn(Optional.of(account));

        assertThrows(AccountAccessDeniedException.class, () -> accountService.validateAccountOwnership(10L, 42L));
        verify(accountRepository).findById(42L);
        verifyNoMoreInteractions(accountRepository, identityRepository, organizationRepository);
    }

    @Test
    void validateAccountOrgAccess_DoesNotThrow_WhenAccountHasOrganizationAccess() {
        AccountService accountService = new AccountService(accountRepository, identityRepository, organizationRepository);

        when(accountRepository.existsByIdAndOrganizationsId(10L, 20L)).thenReturn(true);

        accountService.validateAccountOrgAccess(10L, 20L);

        verify(accountRepository).existsByIdAndOrganizationsId(10L, 20L);
        verifyNoMoreInteractions(accountRepository, identityRepository, organizationRepository);
    }

    @Test
    void validateAccountOrgAccess_ThrowsAccountAccessDeniedException_WhenAccountLacksOrganizationAccess() {
        AccountService accountService = new AccountService(accountRepository, identityRepository, organizationRepository);

        when(accountRepository.existsByIdAndOrganizationsId(10L, 20L)).thenReturn(false);

        assertThrows(AccountAccessDeniedException.class,
                () -> accountService.validateAccountOrgAccess(10L, 20L));

        verify(accountRepository).existsByIdAndOrganizationsId(10L, 20L);
        verifyNoMoreInteractions(accountRepository, identityRepository, organizationRepository);
    }

    @Test
    void resolveOrganizationForAccount_ReturnsOrganizationWhenLinkedToAccount() {
        AccountService accountService = new AccountService(accountRepository, identityRepository, organizationRepository);

        Organization organization = new Organization();
        organization.setId(7L);

        when(organizationRepository.findById(7L)).thenReturn(Optional.of(organization));
        when(accountRepository.existsByIdAndOrganizationsId(42L, 7L)).thenReturn(true);

        Organization result = accountService.resolveOrganizationForAccount(42L, 7L);

        assertEquals(organization, result);
        verify(organizationRepository).findById(7L);
        verify(accountRepository).existsByIdAndOrganizationsId(42L, 7L);
        verifyNoMoreInteractions(accountRepository, identityRepository, organizationRepository);
    }

    @Test
    void resolveOrganizationForAccount_ThrowsValidationErrorWhenOrganizationDoesNotExist() {
        AccountService accountService = new AccountService(accountRepository, identityRepository, organizationRepository);

        when(organizationRepository.findById(7L)).thenReturn(Optional.empty());

        InvalidRequestException exception = assertThrows(
            InvalidRequestException.class,
            () -> accountService.resolveOrganizationForAccount(42L, 7L)
        );

        assertEquals("Organization not found: 7", exception.getMessage());
        verify(organizationRepository).findById(7L);
        verifyNoMoreInteractions(accountRepository, identityRepository, organizationRepository);
    }

    @Test
    void resolveOrganizationForAccount_ThrowsAccessDeniedWhenOrganizationIsNotLinkedToAccount() {
        AccountService accountService = new AccountService(accountRepository, identityRepository, organizationRepository);

        Organization organization = new Organization();
        organization.setId(7L);

        when(organizationRepository.findById(7L)).thenReturn(Optional.of(organization));
        when(accountRepository.existsByIdAndOrganizationsId(42L, 7L)).thenReturn(false);

        assertThrows(AccountAccessDeniedException.class,
            () -> accountService.resolveOrganizationForAccount(42L, 7L)
        );

        verify(organizationRepository).findById(7L);
        verify(accountRepository).existsByIdAndOrganizationsId(42L, 7L);
        verifyNoMoreInteractions(accountRepository, identityRepository, organizationRepository);
    }
}

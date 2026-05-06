package de.ni0.chronoscope.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.keycloak.representations.idm.UserRepresentation;

import de.ni0.chronoscope.controller.dto.response.AccountLinkConfirmResponse;
import de.ni0.chronoscope.exception.AccountAccessDeniedException;
import de.ni0.chronoscope.exception.AccountNotFoundException;
import de.ni0.chronoscope.exception.ResourceNotFoundException;
import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.Identity;
import de.ni0.chronoscope.repository.AccountRepository;
import de.ni0.chronoscope.repository.IdentityRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class IdentityServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private IdentityRepository identityRepository;

    @Mock
    private KeycloakService keycloakService;

    @Mock
    private JavaMailSender mailSender;

    @Test
    void syncIdentity_ReusesExistingIdentityForSameAccount() {
        IdentityService identityService = new IdentityService(accountRepository, identityRepository, keycloakService, mailSender);

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
        IdentityService identityService = new IdentityService(accountRepository, identityRepository, keycloakService, mailSender);

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
        IdentityService identityService = new IdentityService(accountRepository, identityRepository, keycloakService, mailSender);

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
        IdentityService identityService = new IdentityService(accountRepository, identityRepository, keycloakService, mailSender);

        when(identityRepository.findByIdWithAccountsAndOrganizations(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
            () -> identityService.getIdentity(99L));

        assertEquals("Identity not found: 99", exception.getMessage());
        verify(identityRepository).findByIdWithAccountsAndOrganizations(99L);
        verifyNoMoreInteractions(accountRepository, identityRepository);
    }

    @Test
    void sendLink_LooksUpTargetInKeycloakAndSendsTokenForLocalAccount() {
        IdentityService identityService = new IdentityService(accountRepository, identityRepository, keycloakService, mailSender);

        UserRepresentation targetUser = new UserRepresentation();
        targetUser.setId("keycloak-target-subject");

        Account targetAccount = new Account();
        targetAccount.setId(22L);
        targetAccount.setSubject("keycloak-target-subject");

        when(keycloakService.getAccountByEmail("target@example.com")).thenReturn(targetUser);
        when(accountRepository.findBySubject("keycloak-target-subject")).thenReturn(Optional.of(targetAccount));

        identityService.sendLink(11L, "target@example.com");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();

        assertEquals("noreply@ni0.team", message.getFrom());
        assertEquals("target@example.com", message.getTo()[0]);
        assertEquals("Link your account", message.getSubject());
        assertTrue(message.getText().contains("https://chronoscope.ni0.team/link-account?token="));
        verify(keycloakService).getAccountByEmail("target@example.com");
        verify(accountRepository).findBySubject("keycloak-target-subject");
    }

    @Test
    void sendLink_ThrowsWhenKeycloakUserHasNoLocalAccount() {
        IdentityService identityService = new IdentityService(accountRepository, identityRepository, keycloakService, mailSender);

        UserRepresentation targetUser = new UserRepresentation();
        targetUser.setId("keycloak-missing-subject");

        when(keycloakService.getAccountByEmail("missing@example.com")).thenReturn(targetUser);
        when(accountRepository.findBySubject("keycloak-missing-subject")).thenReturn(Optional.empty());

        AccountNotFoundException exception = assertThrows(
            AccountNotFoundException.class,
            () -> identityService.sendLink(11L, "missing@example.com")
        );

        assertEquals("No local account found for subject: keycloak-missing-subject", exception.getMessage());
        verify(keycloakService).getAccountByEmail("missing@example.com");
        verify(accountRepository).findBySubject("keycloak-missing-subject");
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void mergeAccounts_MovesTargetIdentityAccountsToSourceIdentityAndDeletesOldIdentity() {
        IdentityService identityService = new IdentityService(accountRepository, identityRepository, keycloakService, mailSender);

        Identity sourceIdentity = new Identity();
        sourceIdentity.setId(101L);
        Account sourceAccount = account(11L, "source-subject", sourceIdentity);

        Identity targetIdentity = new Identity();
        targetIdentity.setId(202L);
        Account targetAccount = account(22L, "target-subject", targetIdentity);
        Account linkedTargetAccount = account(33L, "linked-target-subject", targetIdentity);

        String token = requestLinkAndExtractToken(identityService, sourceAccount.getId(), targetAccount);
        when(accountRepository.getReferenceById(sourceAccount.getId())).thenReturn(sourceAccount);
        when(accountRepository.getReferenceById(targetAccount.getId())).thenReturn(targetAccount);

        AccountLinkConfirmResponse response = identityService.mergeAccounts(targetIdentity.getId(), token);

        assertEquals(new AccountLinkConfirmResponse(sourceAccount.getId(), targetAccount.getId(), "merged"), response);
        assertSame(sourceIdentity, targetAccount.getIdentity());
        assertSame(sourceIdentity, linkedTargetAccount.getIdentity());
        verify(accountRepository).save(targetAccount);
        verify(accountRepository).save(linkedTargetAccount);
        verify(identityRepository).delete(targetIdentity);
    }

    @Test
    void mergeAccounts_ThrowsWhenAuthenticatedIdentityIsNotTargetIdentity() {
        IdentityService identityService = new IdentityService(accountRepository, identityRepository, keycloakService, mailSender);

        Identity sourceIdentity = new Identity();
        sourceIdentity.setId(101L);
        Account sourceAccount = account(11L, "source-subject", sourceIdentity);

        Identity targetIdentity = new Identity();
        targetIdentity.setId(202L);
        Account targetAccount = account(22L, "target-subject", targetIdentity);

        String token = requestLinkAndExtractToken(identityService, sourceAccount.getId(), targetAccount);
        when(accountRepository.getReferenceById(sourceAccount.getId())).thenReturn(sourceAccount);
        when(accountRepository.getReferenceById(targetAccount.getId())).thenReturn(targetAccount);

        AccountAccessDeniedException exception = assertThrows(
            AccountAccessDeniedException.class,
            () -> identityService.mergeAccounts(303L, token)
        );

        assertEquals("Only the target account can accept the account merge", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
        verify(identityRepository, never()).delete(any(Identity.class));
    }

    private Account account(Long id, String subject, Identity identity) {
        Account account = new Account();
        account.setId(id);
        account.setSubject(subject);
        account.setIdentity(identity);
        identity.getAccounts().add(account);
        return account;
    }

    private String requestLinkAndExtractToken(IdentityService identityService, long sourceAccountId, Account targetAccount) {
        UserRepresentation targetUser = new UserRepresentation();
        targetUser.setId(targetAccount.getSubject());

        when(keycloakService.getAccountByEmail("target@example.com")).thenReturn(targetUser);
        when(accountRepository.findBySubject(targetAccount.getSubject())).thenReturn(Optional.of(targetAccount));

        identityService.sendLink(sourceAccountId, "target@example.com");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        String text = messageCaptor.getValue().getText();
        return text.substring(text.indexOf("?token=") + "?token=".length(), text.indexOf("\n\nThis link expires"));
    }
}

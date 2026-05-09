package de.ni0.chronoscope.service;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import de.ni0.chronoscope.controller.dto.request.SettingsUpdateRequest;
import de.ni0.chronoscope.controller.dto.response.AccountLinkConfirmResponse;
import de.ni0.chronoscope.exception.AccountAccessDeniedException;
import de.ni0.chronoscope.exception.AccountNotFoundException;
import de.ni0.chronoscope.exception.ResourceNotFoundException;
import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.DynamicTask;
import de.ni0.chronoscope.model.Identity;
import de.ni0.chronoscope.model.WorkSlot;
import de.ni0.chronoscope.repository.AccountRepository;
import de.ni0.chronoscope.repository.IdentityRepository;
import de.ni0.chronoscope.repository.TaskRepository;
import de.ni0.chronoscope.repository.WorkSlotRepository;

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

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private WorkSlotRepository workSlotRepository;

    @Test
    void syncIdentity_ReusesExistingIdentityForSameAccount() {
        IdentityService identityService = new IdentityService(accountRepository, identityRepository, keycloakService, mailSender, taskRepository, workSlotRepository);

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
        IdentityService identityService = new IdentityService(accountRepository, identityRepository, keycloakService, mailSender, taskRepository, workSlotRepository);

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
        IdentityService identityService = new IdentityService(accountRepository, identityRepository, keycloakService, mailSender, taskRepository, workSlotRepository);

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
        IdentityService identityService = new IdentityService(accountRepository, identityRepository, keycloakService, mailSender, taskRepository, workSlotRepository);

        when(identityRepository.findByIdWithAccountsAndOrganizations(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
            () -> identityService.getIdentity(99L));

        assertEquals("Identity not found: 99", exception.getMessage());
        verify(identityRepository).findByIdWithAccountsAndOrganizations(99L);
        verifyNoMoreInteractions(accountRepository, identityRepository);
    }

    @Test
    void sendLink_LooksUpTargetInKeycloakAndSendsTokenForLocalAccount() {
        IdentityService identityService = new IdentityService(accountRepository, identityRepository, keycloakService, mailSender, taskRepository, workSlotRepository);

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
        IdentityService identityService = new IdentityService(accountRepository, identityRepository, keycloakService, mailSender, taskRepository, workSlotRepository);

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
        IdentityService identityService = new IdentityService(accountRepository, identityRepository, keycloakService, mailSender, taskRepository, workSlotRepository);

        Identity sourceIdentity = new Identity();
        sourceIdentity.setId(101L);
        Account sourceAccount = account(11L, "source-subject", sourceIdentity);

        Identity targetIdentity = new Identity();
        targetIdentity.setId(202L);
        Account targetAccount = account(22L, "target-subject", targetIdentity);
        Account linkedTargetAccount = account(33L, "linked-target-subject", targetIdentity);

        DynamicTask task = new DynamicTask();
        task.setIdentity(targetIdentity);
        WorkSlot workSlot = new WorkSlot();
        workSlot.setIdentity(targetIdentity);

        String token = requestLinkAndExtractToken(identityService, sourceAccount.getId(), targetAccount);
        when(accountRepository.getReferenceById(sourceAccount.getId())).thenReturn(sourceAccount);
        when(accountRepository.getReferenceById(targetAccount.getId())).thenReturn(targetAccount);
        when(taskRepository.findByIdentityId(targetIdentity.getId())).thenReturn(List.of(task));
        when(workSlotRepository.findByIdentityId(targetIdentity.getId())).thenReturn(List.of(workSlot));

        AccountLinkConfirmResponse response = identityService.mergeAccounts(targetIdentity.getId(), token);

        assertEquals(new AccountLinkConfirmResponse(sourceAccount.getId(), targetAccount.getId(), "merged"), response);
        assertSame(sourceIdentity, task.getIdentity());
        assertSame(sourceIdentity, workSlot.getIdentity());
        assertSame(sourceIdentity, targetAccount.getIdentity());
        assertSame(sourceIdentity, linkedTargetAccount.getIdentity());
        verify(taskRepository).findByIdentityId(targetIdentity.getId());
        verify(taskRepository).saveAll(List.of(task));
        verify(workSlotRepository).findByIdentityId(targetIdentity.getId());
        verify(workSlotRepository).saveAll(List.of(workSlot));
        verify(accountRepository).save(targetAccount);
        verify(accountRepository).save(linkedTargetAccount);
        verify(identityRepository).delete(targetIdentity);
    }

    @Test
    void mergeAccounts_ThrowsWhenAuthenticatedIdentityIsNotTargetIdentity() {
        IdentityService identityService = new IdentityService(accountRepository, identityRepository, keycloakService, mailSender, taskRepository, workSlotRepository);

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

    @Test
    void updateSettings_UpdatesLanguageOnly() {
        IdentityService identityService = new IdentityService(accountRepository, identityRepository, keycloakService, mailSender, taskRepository, workSlotRepository);

        Identity identity = new Identity();
        identity.setId(99L);
        identity.setLanguage("en_US");
        identity.setTheme("light");

        when(identityRepository.findById(99L)).thenReturn(Optional.of(identity));
        when(identityRepository.save(identity)).thenReturn(identity);

        SettingsUpdateRequest request = new SettingsUpdateRequest(Optional.of("de_DE"), Optional.empty());
        Identity result = identityService.updateSettings(99L, request);

        assertEquals("de_DE", result.getLanguage());
        assertEquals("light", result.getTheme());
        verify(identityRepository).findById(99L);
        verify(identityRepository).save(identity);
    }

    @Test
    void updateSettings_UpdatesThemeOnly() {
        IdentityService identityService = new IdentityService(accountRepository, identityRepository, keycloakService, mailSender, taskRepository, workSlotRepository);

        Identity identity = new Identity();
        identity.setId(99L);
        identity.setLanguage("de_DE");
        identity.setTheme("light");

        when(identityRepository.findById(99L)).thenReturn(Optional.of(identity));
        when(identityRepository.save(identity)).thenReturn(identity);

        SettingsUpdateRequest request = new SettingsUpdateRequest(Optional.empty(), Optional.of("dark"));
        Identity result = identityService.updateSettings(99L, request);

        assertEquals("de_DE", result.getLanguage());
        assertEquals("dark", result.getTheme());
        verify(identityRepository).findById(99L);
        verify(identityRepository).save(identity);
    }

    @Test
    void updateSettings_UpdatesBothLanguageAndTheme() {
        IdentityService identityService = new IdentityService(accountRepository, identityRepository, keycloakService, mailSender, taskRepository, workSlotRepository);

        Identity identity = new Identity();
        identity.setId(99L);
        identity.setLanguage("en_US");
        identity.setTheme("light");

        when(identityRepository.findById(99L)).thenReturn(Optional.of(identity));
        when(identityRepository.save(identity)).thenReturn(identity);

        SettingsUpdateRequest request = new SettingsUpdateRequest(Optional.of("de_DE"), Optional.of("dark"));
        Identity result = identityService.updateSettings(99L, request);

        assertEquals("de_DE", result.getLanguage());
        assertEquals("dark", result.getTheme());
        verify(identityRepository).findById(99L);
        verify(identityRepository).save(identity);
    }

    @Test
    void updateSettings_ThrowsWhenIdentityIsMissing() {
        IdentityService identityService = new IdentityService(accountRepository, identityRepository, keycloakService, mailSender, taskRepository, workSlotRepository);

        when(identityRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> identityService.updateSettings(99L, new SettingsUpdateRequest(Optional.of("de_DE"), Optional.empty()))
        );

        assertEquals("Identity not found: 99", exception.getMessage());
        verify(identityRepository).findById(99L);
        verify(identityRepository, never()).save(any(Identity.class));
    }

    @Test
    void mergeAccounts_InheritsSourceLanguageWhenTargetLanguageIsNull() {
        IdentityService identityService = new IdentityService(accountRepository, identityRepository, keycloakService, mailSender, taskRepository, workSlotRepository);

        Identity sourceIdentity = new Identity();
        sourceIdentity.setId(101L);
        sourceIdentity.setLanguage("de_DE");
        sourceIdentity.setTheme("dark");
        Account sourceAccount = account(11L, "source-subject", sourceIdentity);

        Identity targetIdentity = new Identity();
        targetIdentity.setId(202L);
            targetIdentity.setLanguage(null);
            targetIdentity.setTheme(null);
        Account targetAccount = account(22L, "target-subject", targetIdentity);

        String token = requestLinkAndExtractToken(identityService, sourceAccount.getId(), targetAccount);
        when(accountRepository.getReferenceById(sourceAccount.getId())).thenReturn(sourceAccount);
        when(accountRepository.getReferenceById(targetAccount.getId())).thenReturn(targetAccount);
        when(taskRepository.findByIdentityId(targetIdentity.getId())).thenReturn(List.of());
        when(workSlotRepository.findByIdentityId(targetIdentity.getId())).thenReturn(List.of());

        identityService.mergeAccounts(targetIdentity.getId(), token);

        assertEquals("de_DE", sourceIdentity.getLanguage());
        assertEquals("dark", sourceIdentity.getTheme());
        verify(identityRepository).save(sourceIdentity);
        verify(identityRepository).delete(targetIdentity);
    }

    @Test
    void mergeAccounts_PreservesTargetLanguageWhenAlreadySet() {
        IdentityService identityService = new IdentityService(accountRepository, identityRepository, keycloakService, mailSender, taskRepository, workSlotRepository);

        Identity sourceIdentity = new Identity();
        sourceIdentity.setId(101L);
        sourceIdentity.setLanguage("de_DE");
        sourceIdentity.setTheme("dark");
        Account sourceAccount = account(11L, "source-subject", sourceIdentity);

        Identity targetIdentity = new Identity();
        targetIdentity.setId(202L);
        targetIdentity.setLanguage("en_US");
        targetIdentity.setTheme("light");
        Account targetAccount = account(22L, "target-subject", targetIdentity);

        String token = requestLinkAndExtractToken(identityService, sourceAccount.getId(), targetAccount);
        when(accountRepository.getReferenceById(sourceAccount.getId())).thenReturn(sourceAccount);
        when(accountRepository.getReferenceById(targetAccount.getId())).thenReturn(targetAccount);
        when(taskRepository.findByIdentityId(targetIdentity.getId())).thenReturn(List.of());
        when(workSlotRepository.findByIdentityId(targetIdentity.getId())).thenReturn(List.of());

        identityService.mergeAccounts(targetIdentity.getId(), token);

        assertEquals("en_US", sourceIdentity.getLanguage());
        assertEquals("light", sourceIdentity.getTheme());
        verify(identityRepository).save(sourceIdentity);
        verify(identityRepository).delete(targetIdentity);
    }
}
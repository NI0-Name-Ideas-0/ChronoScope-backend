package de.ni0.chronoscope.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import de.ni0.chronoscope.model.ColorToken;
import de.ni0.chronoscope.model.DynamicTask;
import de.ni0.chronoscope.model.Identity;
import de.ni0.chronoscope.model.IdentityOrganizationColor;
import de.ni0.chronoscope.model.WorkSettings;
import de.ni0.chronoscope.model.WorkSlot;
import de.ni0.chronoscope.repository.AccountRepository;
import de.ni0.chronoscope.repository.IdentityOrganizationColorRepository;
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
    private IdentityOrganizationColorRepository identityOrganizationColorRepository;

    @Mock
    private de.ni0.chronoscope.repository.IdentitySettingsRepository identitySettingsRepository;

    @Mock
    private KeycloakService keycloakService;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private WorkSlotRepository workSlotRepository;

    private IdentityService identityServiceWithColors() {
        return new IdentityService(
            accountRepository,
            identityRepository,
            identitySettingsRepository,
            identityOrganizationColorRepository,
            keycloakService,
            mailSender,
            taskRepository,
            workSlotRepository
        );
    }

    @Test
    void syncIdentity_ReusesExistingIdentityForSameAccount() {
        IdentityService identityService = identityServiceWithColors();

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
        IdentityService identityService = identityServiceWithColors();

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
        IdentityService identityService = identityServiceWithColors();

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
        IdentityService identityService = identityServiceWithColors();

        when(identityRepository.findByIdWithAccountsAndOrganizations(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
            () -> identityService.getIdentity(99L));

        assertEquals("Identity not found: 99", exception.getMessage());
        verify(identityRepository).findByIdWithAccountsAndOrganizations(99L);
        verifyNoMoreInteractions(accountRepository, identityRepository);
    }

    @Test
    void sendLink_LooksUpTargetInKeycloakAndSendsTokenForLocalAccount() {
        IdentityService identityService = identityServiceWithColors();

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
        IdentityService identityService = identityServiceWithColors();

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
        IdentityService identityService = identityServiceWithColors();

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
        IdentityService identityService = identityServiceWithColors();

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
        IdentityService identityService = identityServiceWithColors();
        Identity identity = new Identity();
        identity.setId(99L);

        when(identityRepository.findById(99L)).thenReturn(Optional.of(identity));

        SettingsUpdateRequest request = new SettingsUpdateRequest(Optional.of("de_DE"), Optional.empty(), Optional.empty());
        identityService.updateSettings(99L, request);

        var captor = ArgumentCaptor.forClass(de.ni0.chronoscope.model.IdentitySettings.class);
        verify(identitySettingsRepository).save(captor.capture());
        assertEquals("de_DE", captor.getValue().getLanguage());
        verify(identityRepository).save(identity);
    }

    @Test
    void updateSettings_UpdatesThemeOnly() {
        IdentityService identityService = identityServiceWithColors();
        Identity identity = new Identity();
        identity.setId(99L);

        when(identityRepository.findById(99L)).thenReturn(Optional.of(identity));

        SettingsUpdateRequest request = new SettingsUpdateRequest(Optional.empty(), Optional.of("dark"), Optional.empty());
        identityService.updateSettings(99L, request);

        var captor = ArgumentCaptor.forClass(de.ni0.chronoscope.model.IdentitySettings.class);
        verify(identitySettingsRepository).save(captor.capture());
        assertEquals("dark", captor.getValue().getTheme());
        verify(identityRepository).save(identity);
    }

    @Test
    void updateSettings_UpdatesBothLanguageAndTheme() {
        IdentityService identityService = identityServiceWithColors();
        Identity identity = new Identity();
        identity.setId(99L);

        when(identityRepository.findById(99L)).thenReturn(Optional.of(identity));

        SettingsUpdateRequest request = new SettingsUpdateRequest(Optional.of("de_DE"), Optional.of("dark"), Optional.empty());
        identityService.updateSettings(99L, request);

        var captor = ArgumentCaptor.forClass(de.ni0.chronoscope.model.IdentitySettings.class);
        verify(identitySettingsRepository).save(captor.capture());
        assertEquals("de_DE", captor.getValue().getLanguage());
        assertEquals("dark", captor.getValue().getTheme());
        verify(identityRepository).save(identity);
    }

    @Test
    void updateSettings_UpdatesWorkSettings() {
        IdentityService identityService = identityServiceWithColors();
        Identity identity = new Identity();
        identity.setId(99L);

        when(identityRepository.findById(99L)).thenReturn(Optional.of(identity));
        when(identitySettingsRepository.findByIdentityId(99L)).thenReturn(Optional.empty());

        WorkSettings workSettings = new WorkSettings(480, Set.of("mo", "di", "mi", "do", "fr"));
        SettingsUpdateRequest request = new SettingsUpdateRequest(Optional.empty(), Optional.empty(), Optional.of(workSettings));
        identityService.updateSettings(99L, request);

        var captor = ArgumentCaptor.forClass(de.ni0.chronoscope.model.IdentitySettings.class);
        verify(identitySettingsRepository).save(captor.capture());
        assertEquals(workSettings, captor.getValue().getWorkSettings());
        verify(identityRepository).save(identity);
    }

    @Test
    void updateSettings_LeavesExistingValuesWhenRequestIsEmpty() {
        IdentityService identityService = identityServiceWithColors();
        Identity identity = new Identity();
        identity.setId(99L);

        when(identityRepository.findById(99L)).thenReturn(Optional.of(identity));

        SettingsUpdateRequest request = new SettingsUpdateRequest(Optional.empty(), Optional.empty(), Optional.empty());
        identityService.updateSettings(99L, request);

        // No settings saved when request has no updates
        verify(identitySettingsRepository, never()).save(any());
        verify(identityRepository).save(identity);
    }

    @Test
    void updateSettings_ThrowsWhenIdentityIsMissing() {
        IdentityService identityService = identityServiceWithColors();

        when(identityRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> identityService.updateSettings(99L, new SettingsUpdateRequest(Optional.of("de_DE"), Optional.empty(), Optional.empty()))
        );

        assertEquals("Identity not found: 99", exception.getMessage());
        verify(identityRepository).findById(99L);
        verify(identityRepository, never()).save(any(Identity.class));
    }

    @Test
    void mergeAccounts_InheritsSourceLanguageWhenTargetLanguageIsNull() {
        IdentityService identityService = identityServiceWithColors();
        Identity sourceIdentity = new Identity();
        sourceIdentity.setId(101L);
        Account sourceAccount = account(11L, "source-subject", sourceIdentity);

        Identity targetIdentity = new Identity();
        targetIdentity.setId(202L);
        Account targetAccount = account(22L, "target-subject", targetIdentity);

        var sourceSettings = new de.ni0.chronoscope.model.IdentitySettings();
        sourceSettings.setIdentity(sourceIdentity);
        sourceSettings.setLanguage("de_DE");
        sourceSettings.setTheme("dark");

        when(identitySettingsRepository.findByIdentityId(sourceIdentity.getId())).thenReturn(Optional.of(sourceSettings));
        when(identitySettingsRepository.findByIdentityId(targetIdentity.getId())).thenReturn(Optional.empty());

        String token = requestLinkAndExtractToken(identityService, sourceAccount.getId(), targetAccount);
        when(accountRepository.getReferenceById(sourceAccount.getId())).thenReturn(sourceAccount);
        when(accountRepository.getReferenceById(targetAccount.getId())).thenReturn(targetAccount);
        when(taskRepository.findByIdentityId(targetIdentity.getId())).thenReturn(List.of());
        when(workSlotRepository.findByIdentityId(targetIdentity.getId())).thenReturn(List.of());

        identityService.mergeAccounts(targetIdentity.getId(), token);

        // source settings existed and target had none — nothing to move, new identity keeps its settings
        verify(identitySettingsRepository, never()).save(any());
        verify(identityRepository).delete(targetIdentity);
    }

    @Test
    void mergeAccounts_PreservesTargetLanguageWhenAlreadySet() {
        IdentityService identityService = identityServiceWithColors();

        Identity sourceIdentity = new Identity();
        sourceIdentity.setId(101L);
        Account sourceAccount = account(11L, "source-subject", sourceIdentity);

        Identity targetIdentity = new Identity();
        targetIdentity.setId(202L);
        Account targetAccount = account(22L, "target-subject", targetIdentity);

        var sourceSettings = new de.ni0.chronoscope.model.IdentitySettings();
        sourceSettings.setIdentity(sourceIdentity);
        sourceSettings.setLanguage("de_DE");
        sourceSettings.setTheme("dark");

        var targetSettings = new de.ni0.chronoscope.model.IdentitySettings();
        targetSettings.setIdentity(targetIdentity);
        targetSettings.setLanguage("en_US");
        targetSettings.setTheme("light");

        when(identitySettingsRepository.findByIdentityId(sourceIdentity.getId())).thenReturn(Optional.of(sourceSettings));
        when(identitySettingsRepository.findByIdentityId(targetIdentity.getId())).thenReturn(Optional.of(targetSettings));

        String token = requestLinkAndExtractToken(identityService, sourceAccount.getId(), targetAccount);
        when(accountRepository.getReferenceById(sourceAccount.getId())).thenReturn(sourceAccount);
        when(accountRepository.getReferenceById(targetAccount.getId())).thenReturn(targetAccount);
        when(taskRepository.findByIdentityId(targetIdentity.getId())).thenReturn(List.of());
        when(workSlotRepository.findByIdentityId(targetIdentity.getId())).thenReturn(List.of());

        identityService.mergeAccounts(targetIdentity.getId(), token);

        var captor = ArgumentCaptor.forClass(de.ni0.chronoscope.model.IdentitySettings.class);
        verify(identitySettingsRepository).save(captor.capture());
        assertEquals("en_US", captor.getValue().getLanguage());
        assertEquals("light", captor.getValue().getTheme());
        verify(identitySettingsRepository).delete(sourceSettings);
        verify(identityRepository).delete(targetIdentity);
    }

    @Test
    void mergeAccounts_PreservesSourceWorkSettingsWhenTargetWorkSettingsIsNull() {
        IdentityService identityService = identityServiceWithColors();

        Identity sourceIdentity = new Identity();
        sourceIdentity.setId(101L);
        Account sourceAccount = account(11L, "source-subject", sourceIdentity);

        Identity targetIdentity = new Identity();
        targetIdentity.setId(202L);
        Account targetAccount = account(22L, "target-subject", targetIdentity);

        var sourceSettings = new de.ni0.chronoscope.model.IdentitySettings();
        sourceSettings.setIdentity(sourceIdentity);
        sourceSettings.setWorkSettings(new WorkSettings(300, Set.of("mo", "di", "mi")));

        when(identitySettingsRepository.findByIdentityId(sourceIdentity.getId())).thenReturn(Optional.of(sourceSettings));
        when(identitySettingsRepository.findByIdentityId(targetIdentity.getId())).thenReturn(Optional.empty());

        String token = requestLinkAndExtractToken(identityService, sourceAccount.getId(), targetAccount);
        when(accountRepository.getReferenceById(sourceAccount.getId())).thenReturn(sourceAccount);
        when(accountRepository.getReferenceById(targetAccount.getId())).thenReturn(targetAccount);
        when(taskRepository.findByIdentityId(targetIdentity.getId())).thenReturn(List.of());
        when(workSlotRepository.findByIdentityId(targetIdentity.getId())).thenReturn(List.of());

        identityService.mergeAccounts(targetIdentity.getId(), token);
        // target had no settings; nothing to move
        verify(identitySettingsRepository, never()).save(any());
        verify(identityRepository).delete(targetIdentity);
    }

    @Test
    void mergeAccounts_OverridesSourceWorkSettingsWhenTargetWorkSettingsIsSet() {
        IdentityService identityService = identityServiceWithColors();

        Identity sourceIdentity = new Identity();
        sourceIdentity.setId(101L);
        Account sourceAccount = account(11L, "source-subject", sourceIdentity);

        Identity targetIdentity = new Identity();
        targetIdentity.setId(202L);
        Account targetAccount = account(22L, "target-subject", targetIdentity);

        WorkSettings targetSettings = new WorkSettings(480, Set.of("mo", "di", "mi", "do", "fr"));
        var targetSettingsRow = new de.ni0.chronoscope.model.IdentitySettings();
        targetSettingsRow.setIdentity(targetIdentity);
        targetSettingsRow.setWorkSettings(targetSettings);

        when(identitySettingsRepository.findByIdentityId(sourceIdentity.getId())).thenReturn(Optional.empty());
        when(identitySettingsRepository.findByIdentityId(targetIdentity.getId())).thenReturn(Optional.of(targetSettingsRow));

        String token = requestLinkAndExtractToken(identityService, sourceAccount.getId(), targetAccount);
        when(accountRepository.getReferenceById(sourceAccount.getId())).thenReturn(sourceAccount);
        when(accountRepository.getReferenceById(targetAccount.getId())).thenReturn(targetAccount);
        when(taskRepository.findByIdentityId(targetIdentity.getId())).thenReturn(List.of());
        when(workSlotRepository.findByIdentityId(targetIdentity.getId())).thenReturn(List.of());

        identityService.mergeAccounts(targetIdentity.getId(), token);

        var captor = ArgumentCaptor.forClass(de.ni0.chronoscope.model.IdentitySettings.class);
        verify(identitySettingsRepository).save(captor.capture());
        assertEquals(targetSettings, captor.getValue().getWorkSettings());
        verify(identityRepository).delete(targetIdentity);
    }

    @Test
    void getOrganizationColors_ReturnsMappedRows() {
        IdentityService identityService = identityServiceWithColors();
        Identity identity = new Identity();
        identity.setId(99L);

        IdentityOrganizationColor color = new IdentityOrganizationColor();
        color.setIdentity(identity);
        color.setOrganizationId("org-1");
        color.setColor(ColorToken.BLUE);

        when(identityOrganizationColorRepository.findAllByIdentityId(99L)).thenReturn(List.of(color));

        List<IdentityOrganizationColor> result = identityService.getOrganizationColors(99L);

        assertEquals(List.of(color), result);
        verify(identityOrganizationColorRepository).findAllByIdentityId(99L);
    }

    @Test
    void getOrganizationColor_ReturnsExistingColor() {
        IdentityService identityService = identityServiceWithColors();
        Identity identity = new Identity();
        identity.setId(77L);

        IdentityOrganizationColor existing = new IdentityOrganizationColor();
        existing.setIdentity(identity);
        existing.setOrganizationId("org-1");
        existing.setColor(ColorToken.BLUE);

        when(identityOrganizationColorRepository.findByIdentityIdAndOrganizationId(77L, "org-1")).thenReturn(Optional.of(existing));

        IdentityOrganizationColor result = identityService.getOrganizationColor(77L, "org-1");

        assertEquals(existing, result);
        assertEquals(ColorToken.BLUE, result.getColor());
        verify(identityOrganizationColorRepository).findByIdentityIdAndOrganizationId(77L, "org-1");
        verify(identityOrganizationColorRepository, never()).save(any());
    }

    @Test
    void getOrganizationColor_CreatesNewColorWhenNotExists() {
        IdentityService identityService = identityServiceWithColors();
        Identity identity = new Identity();
        identity.setId(77L);

        when(identityOrganizationColorRepository.findByIdentityIdAndOrganizationId(77L, "org-1")).thenReturn(Optional.empty());
        when(identityRepository.getReferenceById(77L)).thenReturn(identity);
        when(identityOrganizationColorRepository.save(any(IdentityOrganizationColor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IdentityOrganizationColor result = identityService.getOrganizationColor(77L, "org-1");

        assertEquals(identity, result.getIdentity());
        assertEquals("org-1", result.getOrganizationId());
        assertNotNull(result.getColor());
        verify(identityOrganizationColorRepository).save(result);
    }

    @Test
    void upsertOrganizationColor_CreatesRowWhenMissing() {
        IdentityService identityService = identityServiceWithColors();
        Identity identity = new Identity();
        identity.setId(77L);

        when(identityRepository.findById(77L)).thenReturn(Optional.of(identity));
        when(identityOrganizationColorRepository.findByIdentityIdAndOrganizationId(77L, "org-1")).thenReturn(Optional.empty());
        when(identityOrganizationColorRepository.save(any(IdentityOrganizationColor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IdentityOrganizationColor result = identityService.upsertOrganizationColor(77L, "org-1", ColorToken.PURPLE);

        assertEquals(identity, result.getIdentity());
        assertEquals("org-1", result.getOrganizationId());
        assertEquals(ColorToken.PURPLE, result.getColor());
        verify(identityOrganizationColorRepository).save(result);
    }

    @Test
    void upsertOrganizationColor_UpdatesExistingRow() {
        IdentityService identityService = identityServiceWithColors();
        Identity identity = new Identity();
        identity.setId(77L);

        IdentityOrganizationColor existing = new IdentityOrganizationColor();
        existing.setIdentity(identity);
        existing.setOrganizationId("org-1");
        existing.setColor(ColorToken.BLUE);

        when(identityRepository.findById(77L)).thenReturn(Optional.of(identity));
        when(identityOrganizationColorRepository.findByIdentityIdAndOrganizationId(77L, "org-1")).thenReturn(Optional.of(existing));
        when(identityOrganizationColorRepository.save(any(IdentityOrganizationColor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IdentityOrganizationColor result = identityService.upsertOrganizationColor(77L, "org-1", ColorToken.GREEN);

        assertEquals(existing, result);
        assertEquals(ColorToken.GREEN, existing.getColor());
        verify(identityOrganizationColorRepository).save(existing);
    }

    @Test
    void deleteOrganizationColor_DeletesMappedRow() {
        IdentityService identityService = identityServiceWithColors();

        identityService.deleteOrganizationColor(55L, "org-1");

        verify(identityOrganizationColorRepository).deleteByIdentityIdAndOrganizationId(55L, "org-1");
    }

    @Test
    void mergeAccounts_MigratesOrganizationColorsToNewIdentity() {
        IdentityService identityService = identityServiceWithColors();

        Identity sourceIdentity = new Identity();
        sourceIdentity.setId(101L);
        Account sourceAccount = account(11L, "source-subject", sourceIdentity);

        Identity targetIdentity = new Identity();
        targetIdentity.setId(202L);
        Account targetAccount = account(22L, "target-subject", targetIdentity);

        IdentityOrganizationColor targetColor = new IdentityOrganizationColor();
        targetColor.setIdentity(targetIdentity);
        targetColor.setOrganizationId("org-1");
        targetColor.setColor(ColorToken.ORANGE);

        when(identityOrganizationColorRepository.findAllByIdentityId(targetIdentity.getId())).thenReturn(List.of(targetColor));

        String token = requestLinkAndExtractToken(identityService, sourceAccount.getId(), targetAccount);
        when(accountRepository.getReferenceById(sourceAccount.getId())).thenReturn(sourceAccount);
        when(accountRepository.getReferenceById(targetAccount.getId())).thenReturn(targetAccount);
        when(taskRepository.findByIdentityId(targetIdentity.getId())).thenReturn(List.of());
        when(workSlotRepository.findByIdentityId(targetIdentity.getId())).thenReturn(List.of());

        identityService.mergeAccounts(targetIdentity.getId(), token);

        assertEquals(sourceIdentity, targetColor.getIdentity());
        verify(identityOrganizationColorRepository).save(targetColor);
        verify(identityRepository).delete(targetIdentity);
    }

    @Test
    void mergeAccounts_PrefersTargetOrganizationColorWhenBothIdentitiesHaveOne() {
        IdentityService identityService = identityServiceWithColors();

        Identity sourceIdentity = new Identity();
        sourceIdentity.setId(101L);
        Account sourceAccount = account(11L, "source-subject", sourceIdentity);

        Identity targetIdentity = new Identity();
        targetIdentity.setId(202L);
        Account targetAccount = account(22L, "target-subject", targetIdentity);

        IdentityOrganizationColor targetColor = new IdentityOrganizationColor();
        targetColor.setIdentity(targetIdentity);
        targetColor.setOrganizationId("org-1");
        targetColor.setColor(ColorToken.ORANGE);

        IdentityOrganizationColor sourceColor = new IdentityOrganizationColor();
        sourceColor.setIdentity(sourceIdentity);
        sourceColor.setOrganizationId("org-1");
        sourceColor.setColor(ColorToken.PURPLE);

        when(identityOrganizationColorRepository.findAllByIdentityId(targetIdentity.getId())).thenReturn(List.of(targetColor));
        when(identityOrganizationColorRepository.findByIdentityIdAndOrganizationId(sourceIdentity.getId(), "org-1")).thenReturn(Optional.of(sourceColor));

        String token = requestLinkAndExtractToken(identityService, sourceAccount.getId(), targetAccount);
        when(accountRepository.getReferenceById(sourceAccount.getId())).thenReturn(sourceAccount);
        when(accountRepository.getReferenceById(targetAccount.getId())).thenReturn(targetAccount);
        when(taskRepository.findByIdentityId(targetIdentity.getId())).thenReturn(List.of());
        when(workSlotRepository.findByIdentityId(targetIdentity.getId())).thenReturn(List.of());

        identityService.mergeAccounts(targetIdentity.getId(), token);

        assertEquals(ColorToken.PURPLE, sourceColor.getColor());
        verify(identityRepository).delete(targetIdentity);
    }
}

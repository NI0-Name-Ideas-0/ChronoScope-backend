package de.ni0.chronoscope.service;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.ni0.chronoscope.controller.dto.request.SettingsUpdateRequest;
import de.ni0.chronoscope.controller.dto.response.AccountLinkConfirmResponse;
import de.ni0.chronoscope.exception.AccountAccessDeniedException;
import de.ni0.chronoscope.exception.AccountNotFoundException;
import de.ni0.chronoscope.exception.ResourceNotFoundException;
import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.ColorToken;
import de.ni0.chronoscope.model.Identity;
import de.ni0.chronoscope.model.IdentityOrganizationColor;
import de.ni0.chronoscope.model.Task;
import de.ni0.chronoscope.model.WorkSlot;
import de.ni0.chronoscope.repository.AccountRepository;
import de.ni0.chronoscope.repository.IdentityOrganizationColorRepository;
import de.ni0.chronoscope.repository.IdentityRepository;
import de.ni0.chronoscope.repository.TaskRepository;
import de.ni0.chronoscope.repository.WorkSlotRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

/**
 * Service layer for identity lookup, creation, and account-link workflows.
 */
@Service
@RequiredArgsConstructor
public class IdentityService {

    private final AccountRepository accountRepository;
    private final IdentityRepository identityRepository;
    private final de.ni0.chronoscope.repository.IdentitySettingsRepository identitySettingsRepository;
    private final IdentityOrganizationColorRepository identityOrganizationColorRepository;
    private final KeycloakService keycloakService;
    private final JavaMailSender mailSender;
    private final TaskRepository taskRepository;
    private final WorkSlotRepository workSlotRepository;

    private final SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    /**
     * Synchronizes the identity for the user identified by the given subject.
     *
     * @param subject the external subject from the authentication token
     * @return the resolved or newly created identity ID
     * @throws IllegalStateException if the account for the subject cannot be found
     */
    @Transactional
    public long syncIdentity(String subject) {
        return this.accountRepository.findBySubject(subject)
            .map(account -> {
                if (account.getIdentity() != null) {
                    return account.getIdentity().getId();
                }

                Identity identity = this.identityRepository.save(new Identity());

                // create default settings row for new identity
                var settings = new de.ni0.chronoscope.model.IdentitySettings();
                settings.setIdentity(identity);
                settings.setLanguage("en_US");
                settings.setTheme("light");
                settings.setWorkSettings(new de.ni0.chronoscope.model.WorkSettings(480, Set.of("mo", "di", "mi", "do", "fr")));
                this.identitySettingsRepository.save(settings);

                account.setIdentity(identity);
                this.accountRepository.save(account);

                return identity.getId();
            })
            .orElseThrow(() -> new IllegalStateException("Account not found for subject: " + subject));
    }

    /**
     * Returns the identity with its accounts and organization graph.
     *
     * @param identityId the identity ID
     * @return the loaded identity
     * @throws ResourceNotFoundException if no identity exists for the given ID
     */
    @Transactional(readOnly = true)
    public Identity getIdentity(long identityId) {
        return this.identityRepository.findByIdWithAccountsAndOrganizations(identityId)
            .orElseThrow(() -> new ResourceNotFoundException("Identity not found: " + identityId));
    }

    @Transactional(readOnly = true)
    public List<IdentityOrganizationColor> getOrganizationColors(long identityId) {
        return this.identityOrganizationColorRepository.findAllByIdentityId(identityId);
    }

    @Transactional
    public IdentityOrganizationColor upsertOrganizationColor(long identityId, String organizationId, ColorToken color) {
        Identity identity = this.identityRepository.findById(identityId)
            .orElseThrow(() -> new ResourceNotFoundException("Identity not found: " + identityId));
        if (color == null) {
            throw new IllegalArgumentException("color must be provided");
        }

        var existing = this.identityOrganizationColorRepository.findByIdentityIdAndOrganizationId(identityId, organizationId);
        if (color == ColorToken.UNSET) {
            existing.ifPresent(this.identityOrganizationColorRepository::delete);
            return null;
        }

        IdentityOrganizationColor entity = existing.orElseGet(IdentityOrganizationColor::new);
        entity.setIdentity(identity);
        entity.setOrganizationId(organizationId);
        entity.setColor(color);
        return this.identityOrganizationColorRepository.save(entity);
    }

    @Transactional
    public void deleteOrganizationColor(long identityId, String organizationId) {
        this.identityOrganizationColorRepository.deleteByIdentityIdAndOrganizationId(identityId, organizationId);
    }

    /**
     * Updates the language, theme, and/or work settings for an identity.
     *
     * @param identityId the identity ID
     * @param request the settings update request containing optional values
     * @return the updated identity
     * @throws ResourceNotFoundException if no identity exists for the given ID
     */
    @Transactional
    public Identity updateSettings(long identityId, SettingsUpdateRequest request) {
        Identity identity = this.identityRepository.findById(identityId)
            .orElseThrow(() -> new ResourceNotFoundException("Identity not found: " + identityId));
        boolean hasUpdate = request.language().isPresent() || request.theme().isPresent() || request.workSettings().isPresent();
        if (!hasUpdate) {
            return this.identityRepository.save(identity);
        }

        var settings = this.identitySettingsRepository.findByIdentityId(identityId)
            .orElseGet(() -> {
                var s = new de.ni0.chronoscope.model.IdentitySettings();
                s.setIdentity(identity);
                return s;
            });

        request.language().ifPresent(settings::setLanguage);
        request.theme().ifPresent(settings::setTheme);
        request.workSettings().ifPresent(settings::setWorkSettings);

        this.identitySettingsRepository.save(settings);
        return this.identityRepository.save(identity);
    }

    /**
     * Confirms an account-link token and moves all target identity accounts, tasks, and work slots
     * to the source identity, then deletes the old identity.
     *
     * @param identityId the identity ID of the account accepting the merge
     * @param token signed token produced by {@link #sendLink(long, String)}
     * @return merge result containing the source and target account IDs
     */
    @Transactional
    public AccountLinkConfirmResponse mergeAccounts(long identityId, String token) {
        Claims claims = this.getTokenClaims(token);
        Long sourceId = Long.valueOf(claims.getSubject());
        Long targetId = claims.get("target", Long.class);

        Account sourceAccount = this.accountRepository.getReferenceById(sourceId);
        Account targetAccount = this.accountRepository.getReferenceById(targetId);
        Identity oldIdentity = targetAccount.getIdentity();
        if (identityId != oldIdentity.getId()) {
            throw new AccountAccessDeniedException("Only the target account can accept the account merge");
        }
        Identity newIdentity = sourceAccount.getIdentity();

        List<Task> tasks = this.taskRepository.findByIdentityId(oldIdentity.getId());
        for (Task task : tasks) {
            task.setIdentity(newIdentity);
        }
        this.taskRepository.saveAll(tasks);

        List<WorkSlot> workSlots = this.workSlotRepository.findByIdentityId(oldIdentity.getId());
        for (WorkSlot workSlot : workSlots) {
            workSlot.setIdentity(newIdentity);
        }
        this.workSlotRepository.saveAll(workSlots);

        if (this.identityOrganizationColorRepository != null) {
            List<IdentityOrganizationColor> oldOrganizationColors = this.identityOrganizationColorRepository.findAllByIdentityId(oldIdentity.getId());
            for (IdentityOrganizationColor oldColor : oldOrganizationColors) {
                var newColor = this.identityOrganizationColorRepository.findByIdentityIdAndOrganizationId(newIdentity.getId(), oldColor.getOrganizationId());
                if (newColor.isPresent()) {
                    newColor.get().setColor(oldColor.getColor());
                    this.identityOrganizationColorRepository.save(newColor.get());
                    this.identityOrganizationColorRepository.delete(oldColor);
                } else {
                    oldColor.setIdentity(newIdentity);
                    this.identityOrganizationColorRepository.save(oldColor);
                }
            }
        }

        for (Account account : oldIdentity.getAccounts()) {
            account.setIdentity(newIdentity);
            this.accountRepository.save(account);
        }

        // Merge settings: prefer existing IdentitySettings rows; if none exist, do nothing.
        var oldSettingsOpt = this.identitySettingsRepository.findByIdentityId(oldIdentity.getId());
        var newSettingsOpt = this.identitySettingsRepository.findByIdentityId(newIdentity.getId());

        if (oldSettingsOpt.isPresent()) {
            var oldSettings = oldSettingsOpt.get();
            if (newSettingsOpt.isEmpty()) {
                // move settings row to new identity
                oldSettings.setIdentity(newIdentity);
                this.identitySettingsRepository.save(oldSettings);
            } else {
                var newSettings = newSettingsOpt.get();
                // target (oldSettings) overrides source (newSettings) when present
                if (oldSettings.getLanguage() != null && !oldSettings.getLanguage().isEmpty()) {
                    newSettings.setLanguage(oldSettings.getLanguage());
                }
                if (oldSettings.getTheme() != null && !oldSettings.getTheme().isEmpty()) {
                    newSettings.setTheme(oldSettings.getTheme());
                }
                if (oldSettings.getWorkSettings() != null) {
                    newSettings.setWorkSettings(oldSettings.getWorkSettings());
                }
                this.identitySettingsRepository.save(newSettings);
                this.identitySettingsRepository.delete(oldSettings);
            }
        }
        this.identityRepository.save(newIdentity);
        this.identityRepository.delete(oldIdentity);

        return new AccountLinkConfirmResponse(sourceId, targetId, "merged");
    }

    /**
     * Returns the settings for the given identity, creating a default view if none exist yet.
     *
     * @param identityId the identity ID
     * @return the loaded settings
     * @throws ResourceNotFoundException if no identity exists for the given ID
     */
    @Transactional(readOnly = true)
    public de.ni0.chronoscope.model.IdentitySettings getSettings(long identityId) {
        return this.identitySettingsRepository.findByIdentityId(identityId)
            .orElseGet(() -> {
                Identity identity = this.identityRepository.findById(identityId)
                    .orElseThrow(() -> new ResourceNotFoundException("Identity not found: " + identityId));
                var s = new de.ni0.chronoscope.model.IdentitySettings();
                s.setIdentity(identity);
                s.setLanguage("en_US");
                s.setTheme("light");
                s.setWorkSettings(new de.ni0.chronoscope.model.WorkSettings(480, Set.of("mo", "di", "mi", "do", "fr")));
                return s;
            });
    }

    /**
     * Sends a short-lived account-link confirmation mail to another account.
     *
     * @param accountId source account requesting the merge
     * @param targetMail e-mail address of the target account
     */
    public void sendLink(long accountId, String targetMail) {
        UserRepresentation targetUser = this.keycloakService.getAccountByEmail(targetMail);
        Account targetAccount = this.accountRepository.findBySubject(targetUser.getId())
                .orElseThrow(() -> new AccountNotFoundException("No local account found for subject: " + targetUser.getId()));
        String token = generateLinkToken(accountId, targetAccount.getId());
        this.sendLinkEmail(targetMail, token);
    }

    private void sendLinkEmail(String to, String token) {
        String link = "https://chronoscope.ni0.team/link-account?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@ni0.team");
        message.setTo(to);
        message.setSubject("Link your account");
        message.setText(
                "Click the link to link your account:\n\n" +
                        link +
                        "\n\nThis link expires in 15 minutes."
        );

        mailSender.send(message);
    }

    private String generateLinkToken(long sourceId, long targetId) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .setSubject(String.valueOf(sourceId))
                .claim("target", targetId)
                .setId(UUID.randomUUID().toString()) // jti
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + 15 * 60 * 1000))
                .signWith(key)
                .compact();
    }

    private Claims getTokenClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }
}

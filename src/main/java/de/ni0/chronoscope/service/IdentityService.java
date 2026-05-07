package de.ni0.chronoscope.service;

import de.ni0.chronoscope.controller.dto.response.AccountLinkConfirmResponse;
import de.ni0.chronoscope.exception.AccountAccessDeniedException;
import de.ni0.chronoscope.exception.AccountNotFoundException;
import de.ni0.chronoscope.model.Account;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.ni0.chronoscope.exception.ResourceNotFoundException;
import de.ni0.chronoscope.model.Identity;
import de.ni0.chronoscope.repository.AccountRepository;
import de.ni0.chronoscope.repository.IdentityRepository;
import lombok.RequiredArgsConstructor;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * Service layer for identity lookup, creation, and account-link workflows.
 */
@Service
@RequiredArgsConstructor
public class IdentityService {
    private final SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    private final AccountRepository accountRepository;
    private final IdentityRepository identityRepository;
    private final KeycloakService keycloakService;

    private final JavaMailSender mailSender;

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
                account.setIdentity(identity);
                this.accountRepository.save(account);

                return identity.getId();
            })
            .orElseThrow(() -> new IllegalStateException("Account not found for subject: " + subject));
    }

    /**
     * Returns the identity with its accounts and organizationId graph.
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

    /**
     * Confirms an account-link token and moves all target identity accounts to the source identity.
     *
     * @param token signed token produced by {@link #sendLink(long, String)}
     * @return merge result containing the source and target account IDs
     */
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
        for (Account account : oldIdentity.getAccounts()) {
            account.setIdentity(sourceAccount.getIdentity());
            this.accountRepository.save(account);
        }

        this.identityRepository.delete(oldIdentity);

        return new AccountLinkConfirmResponse(sourceId, targetId, "merged");
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
                .setExpiration(new Date(now + 1000 * 60 * 15)) // 15 min
                .signWith(key)
                .compact();
    }

    private Claims getTokenClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}

package de.ni0.chronoscope.service;

import de.ni0.chronoscope.controller.dto.response.AccountLinkConfirmResponse;
import de.ni0.chronoscope.exception.AccountNotFoundException;
import de.ni0.chronoscope.model.Account;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
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
import java.util.Optional;
import java.util.UUID;

/**
 * Service layer for identity operations.
 */
@Service
@RequiredArgsConstructor
public class IdentityService {
    private final SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    private final AccountRepository accountRepository;
    private final IdentityRepository identityRepository;

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

    public AccountLinkConfirmResponse mergeAccounts(String token) {
        Claims claims = this.getTokenClaims(token);
        Long sourceId = Long.valueOf(claims.getSubject());
        Long targetId = claims.get("target", Long.class);

        Account sourceAccount = this.accountRepository.getReferenceById(sourceId);
        Account targetAccount = this.accountRepository.getReferenceById(targetId);
        Identity oldIdentity = targetAccount.getIdentity();
        targetAccount.setIdentity(sourceAccount.getIdentity());

        this.identityRepository.delete(oldIdentity);
        this.accountRepository.save(targetAccount);

        return new AccountLinkConfirmResponse(sourceId, targetId, "merged");
    }

    public void sendLink(long accountId, String targetMail) {
        Optional<Account> targetAccountOpt = this.accountRepository.findByMail(targetMail);
        if (targetAccountOpt.isEmpty()) {
            throw new AccountNotFoundException();
        }
        Account targetAccount = targetAccountOpt.get();
        String token = generateLinkToken(accountId, targetAccount.getId());
        this.sendLinkEmail(targetAccount.getMail(), token);
    }

    private void sendLinkEmail(String to, String token) {
        String link = "https://chronoscope.ni0.team/link-account?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
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

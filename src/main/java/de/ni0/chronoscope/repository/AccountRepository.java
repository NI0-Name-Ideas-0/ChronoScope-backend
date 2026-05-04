package de.ni0.chronoscope.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import de.ni0.chronoscope.model.Account;

import java.util.Optional;

/**
 * Repository for accounts synchronized from external authentication subjects.
 */
public interface AccountRepository extends JpaRepository<Account, Long> {
    /**
     * Finds an account by its unique external subject.
     *
     * @param subject authentication-provider subject
     * @return matching account, if present
     */
    Optional<Account> findBySubject(String subject);

    /**
     * Finds an account by its e-mail address.
     *
     * @param mail account e-mail
     * @return matching account, if present
     */
    Optional<Account> findByMail(String mail);

    /**
     * Checks whether an account is linked to an organization.
     *
     * @param accountId account ID
     * @param organizationId organization ID
     * @return {@code true} when the account has organization access
     */
    boolean existsByIdAndOrganizationsId(long accountId, long organizationId);
}

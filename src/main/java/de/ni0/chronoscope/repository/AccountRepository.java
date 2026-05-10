package de.ni0.chronoscope.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import de.ni0.chronoscope.model.Account;
import org.springframework.data.jpa.repository.Query;

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
    @Query("""
        select o
        from Account o
        join fetch o.identity as i
        join fetch i.accounts
        where o.subject = :subject
    """)
    Optional<Account> findBySubject(String subject);
}

package de.ni0.chronoscope.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import de.ni0.chronoscope.model.Account;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    java.util.Optional<Account> findBySubject(String subject);

    Optional<Account> findByMail(String mail);

    boolean existsByIdAndOrganizationsId(long accountId, long organizationId);
}

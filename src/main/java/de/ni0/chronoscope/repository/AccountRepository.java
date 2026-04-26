package de.ni0.chronoscope.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import de.ni0.chronoscope.model.Account;

public interface AccountRepository extends JpaRepository<Account, Long> {
    java.util.Optional<Account> findBySubject(String subject);

    boolean existsByIdentityIdAndOrganizationsId(long identityId, long organizationId);
}

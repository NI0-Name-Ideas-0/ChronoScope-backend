package de.ni0.chronoscope.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.Identity;

public interface IdentityRepository extends JpaRepository<Identity, Long> {
    Optional<Identity> findByAccountsContains(Account account);

    @Query("SELECT DISTINCT i FROM Identity i LEFT JOIN FETCH i.accounts a LEFT JOIN FETCH a.organizations WHERE i.id = :id")
    Optional<Identity> findByIdWithAccountsAndOrganizations(@Param("id") Long id);
}

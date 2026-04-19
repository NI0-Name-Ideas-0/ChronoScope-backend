package de.ni0.chronoscope.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.Identity;

public interface IdentityRepository extends JpaRepository<Identity, Long> {
    Optional<Identity> findByAccountsContains(Account account);
}

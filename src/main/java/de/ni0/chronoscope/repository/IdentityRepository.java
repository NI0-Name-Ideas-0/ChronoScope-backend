package de.ni0.chronoscope.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import de.ni0.chronoscope.model.Account;
import de.ni0.chronoscope.model.Identity;

/**
 * Repository for identities and their linked accounts.
 */
public interface IdentityRepository extends JpaRepository<Identity, Long> {
    /**
     * Finds the identity that owns the supplied account.
     *
     * @param account account linked to the identity
     * @return matching identity, if present
     */
    Optional<Identity> findByAccountsContains(Account account);

    /**
     * Finds an identity by ID and eagerly loads the associated accounts and organizations.
     *
     * @param id the identity ID
     * @return the identity with its account and organization graph, if present
     */
    @Query("SELECT DISTINCT i FROM Identity i LEFT JOIN FETCH i.accounts a WHERE i.id = :id")
    Optional<Identity> findByIdWithAccountsAndOrganizations(@Param("id") Long id);
}

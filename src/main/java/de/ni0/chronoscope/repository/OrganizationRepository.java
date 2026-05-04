package de.ni0.chronoscope.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import de.ni0.chronoscope.model.Organization;

import java.util.Optional;

/**
 * Repository for organizations used to scope account access.
 */
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    /**
     * Finds an organization by its unique name.
     *
     * @param name organization name
     * @return matching organization, if present
     */
    Optional<Organization> findByName(String name);
}

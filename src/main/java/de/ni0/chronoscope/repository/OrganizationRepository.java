package de.ni0.chronoscope.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import de.ni0.chronoscope.model.Organization;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    java.util.Optional<Organization> findByName(String name);
}

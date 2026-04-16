package de.ni0.chronoscope.repository;

import de.ni0.chronoscope.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
}

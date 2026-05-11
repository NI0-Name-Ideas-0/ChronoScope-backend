package de.ni0.chronoscope.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import de.ni0.chronoscope.model.IdentityOrganizationColor;

public interface IdentityOrganizationColorRepository extends JpaRepository<IdentityOrganizationColor, Long> {
    List<IdentityOrganizationColor> findByIdentityId(Long identityId);

    Optional<IdentityOrganizationColor> findByIdentityIdAndOrganizationId(Long identityId, String organizationId);

    void deleteByIdentityIdAndOrganizationId(Long identityId, String organizationId);
}

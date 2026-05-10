package de.ni0.chronoscope.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import de.ni0.chronoscope.model.Identity;
import de.ni0.chronoscope.model.IdentitySettings;

public interface IdentitySettingsRepository extends JpaRepository<IdentitySettings, Long> {
    Optional<IdentitySettings> findByIdentity(Identity identity);
    Optional<IdentitySettings> findByIdentityId(Long identityId);
}

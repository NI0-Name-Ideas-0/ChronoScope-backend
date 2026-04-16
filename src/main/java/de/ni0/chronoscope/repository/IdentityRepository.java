package de.ni0.chronoscope.repository;

import de.ni0.chronoscope.model.Identity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentityRepository extends JpaRepository<Identity, Long> {
}

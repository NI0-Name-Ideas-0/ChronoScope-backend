package de.ni0.chronoscope.repository;

import de.ni0.chronoscope.model.Label;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for task labels.
 */
public interface LabelRepository extends JpaRepository<Label, Long> {
}

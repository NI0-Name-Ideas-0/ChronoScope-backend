package de.ni0.chronoscope.repository;

import de.ni0.chronoscope.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {
}

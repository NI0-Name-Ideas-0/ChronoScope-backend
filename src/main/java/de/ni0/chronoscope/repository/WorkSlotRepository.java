package de.ni0.chronoscope.repository;

import de.ni0.chronoscope.model.WorkSlot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkSlotRepository extends JpaRepository<WorkSlot, Long> {
}

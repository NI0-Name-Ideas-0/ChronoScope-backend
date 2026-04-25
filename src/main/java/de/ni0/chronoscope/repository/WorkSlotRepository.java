package de.ni0.chronoscope.repository;

import de.ni0.chronoscope.model.WorkSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkSlotRepository extends JpaRepository<WorkSlot, Long> {

    List<WorkSlot> findByAccountIdentityId(long identityId);

    Optional<WorkSlot> findByIdAndAccountIdentityId(Long id, long identityId);
}

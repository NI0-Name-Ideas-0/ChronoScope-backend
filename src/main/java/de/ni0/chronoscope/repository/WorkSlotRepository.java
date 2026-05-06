package de.ni0.chronoscope.repository;

import de.ni0.chronoscope.model.WorkSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for account availability windows.
 */
public interface WorkSlotRepository extends JpaRepository<WorkSlot, Long> {

    /**
     * Finds all work slots visible to an identity through linked accounts.
     *
     * @param identityId identity ID
     * @return matching work slots
     */
    List<WorkSlot> findByIdentityId(long identityId);

    /**
     * Finds one work slot while enforcing the identity boundary.
     *
     * @param id work slot ID
     * @param identityId identity ID
     * @return matching work slot, if present
     */
    Optional<WorkSlot> findByIdAndIdentityId(Long id, long identityId);

    List<WorkSlot> findByIdentityIdAndOrganizationId(Long IdentityId, String organization);
}

package de.ni0.chronoscope.mapper;

import de.ni0.chronoscope.model.Organization;
import jakarta.persistence.EntityManager;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

/**
 * Supplies JPA organization references for MapStruct mappings from ID values.
 */
@Component
public class OrganizationProxyProvider {

    private final EntityManager entityManager;

    public OrganizationProxyProvider(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Returns a lazy organization reference without loading the entity immediately.
     *
     * @param organizationId organization ID to reference
     * @return organization proxy
     */
    @Named("organizationProxy")
    public Organization getReference(Long organizationId) {
        return entityManager.getReference(Organization.class, organizationId);
    }
}

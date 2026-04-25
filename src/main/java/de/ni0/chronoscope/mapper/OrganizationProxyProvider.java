package de.ni0.chronoscope.mapper;

import de.ni0.chronoscope.model.Organization;
import jakarta.persistence.EntityManager;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
public class OrganizationProxyProvider {

    private final EntityManager entityManager;

    public OrganizationProxyProvider(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Named("organizationProxy")
    public Organization getReference(Long organizationId) {
        return entityManager.getReference(Organization.class, organizationId);
    }
}

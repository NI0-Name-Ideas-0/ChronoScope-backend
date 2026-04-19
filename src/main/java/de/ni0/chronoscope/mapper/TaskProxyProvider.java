package de.ni0.chronoscope.mapper;

import de.ni0.chronoscope.model.DynamicTask;
import jakarta.persistence.EntityManager;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
public class TaskProxyProvider {

    private final EntityManager entityManager;

    public TaskProxyProvider(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Named("predecessorProxy")
    public DynamicTask getPredecessorReference(Long predecessorId) {
        return entityManager.getReference(DynamicTask.class, predecessorId);
    }
}
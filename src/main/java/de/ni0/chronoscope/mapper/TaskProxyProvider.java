package de.ni0.chronoscope.mapper;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import de.ni0.chronoscope.model.DynamicTask;
import jakarta.persistence.EntityManager;

/**
 * Converts dynamic-task IDs and entity sets for MapStruct dependency mappings.
 */
@Component
public class TaskProxyProvider {

    private final EntityManager entityManager;

    public TaskProxyProvider(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Converts dependency IDs into lazy dynamic task references.
     *
     * @param dependencyIds IDs from a request payload
     * @return dependency reference set
     */
    @Named("dependencyIdsToReferences")
    public Set<DynamicTask> dependencyIdsToReferences(List<Long> dependencyIds) {
        return dependencyIds.stream()
            .map(id -> entityManager.getReference(DynamicTask.class, id))
            .collect(Collectors.toSet());
    }

    /**
     * Converts dynamic task references to sorted ID values for stable responses.
     *
     * @param dynamicTasks dynamic task set
     * @return sorted task IDs
     */
    @Named("dynamicTasksToIds")
    public List<Long> dynamicTasksToIds(Set<DynamicTask> dynamicTasks) {
        return dynamicTasks.stream()
            .map(DynamicTask::getId)
            .sorted()
            .toList();
    }
}

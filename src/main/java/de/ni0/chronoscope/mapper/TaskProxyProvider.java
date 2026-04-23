package de.ni0.chronoscope.mapper;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import de.ni0.chronoscope.model.DynamicTask;
import jakarta.persistence.EntityManager;

@Component
public class TaskProxyProvider {

    private final EntityManager entityManager;

    public TaskProxyProvider(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Named("dependencyIdsToReferences")
    public Set<DynamicTask> dependencyIdsToReferences(List<Long> dependencyIds) {
        return dependencyIds.stream()
            .map(id -> entityManager.getReference(DynamicTask.class, id))
            .collect(Collectors.toSet());
    }

    @Named("dynamicTasksToIds")
    public List<Long> dynamicTasksToIds(Set<DynamicTask> dynamicTasks) {
        return dynamicTasks.stream()
            .map(DynamicTask::getId)
            .sorted()
            .toList();
    }
}
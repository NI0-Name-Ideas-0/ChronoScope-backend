package de.ni0.chronoscope.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import de.ni0.chronoscope.model.DynamicTask;
import de.ni0.chronoscope.model.StaticTask;
import de.ni0.chronoscope.model.Task;

/**
 * Repository for static and dynamic tasks in the joined inheritance hierarchy.
 */
public interface TaskRepository extends JpaRepository<Task, Long> {
    /**
     * Loads all tasks visible to an identity with labels eagerly available for response mapping.
     *
     * @param identityId identity ID
     * @return matching tasks
     */
    @EntityGraph(attributePaths = { "labels" })
    List<Task> findByIdentityId(long identityId);

    /**
     * Loads dynamic tasks visible to an identity with dependency edges eagerly available.
     *
     * @param identityId identity ID
     * @return matching dynamic tasks
     */
    @EntityGraph(attributePaths = { "dependencies", "dependents" })
    List<DynamicTask> findDynamicTasksByIdentityId(long identityId);

    /**
     * Loads dynamic tasks for planning a single identity and organizationId.
     *
     * @param identityId identity ID
     * @param organizationId organizationId ID
     * @return matching dynamic tasks
     */
    @EntityGraph(attributePaths = { "dependencies", "dependents" })
    List<DynamicTask> findDynamicTasksByIdentityIdAndOrganizationId(Long identityId, String organizationId);

    /**
     * Finds one task while enforcing the identity boundary.
     *
     * @param id task ID
     * @param identityId identity ID
     * @return matching task, if present
     */
    Optional<Task> findByIdAndIdentityId(Long id, Long identityId);

    /**
     * Finds dynamic tasks that depend on a given dynamic task within one identity.
     *
     * @param dependencyId dependency task ID
     * @param identityId identity ID
     * @return dependents that reference the dependency
     */
    @Query("""
            select dt
            from DynamicTask dt
            join dt.dependencies dep
            where dep.id = :dependencyId
              and dt.identity.id = :identityId
            """)
    List<DynamicTask> findDependentsByDependencyIdAndIdentityId(@Param("dependencyId") Long dependencyId,
            @Param("identityId") Long identityId);

    /**
     * Loads all static tasks for a given identity, across all organizations.
     * Every static task for the identity blocks work slots regardless of which
     * organization it belongs to.
     *
     * @param identityId identity ID
     * @return all static tasks owned by that identity
     */
    List<StaticTask> findStaticTasksByIdentityId(Long identityId);
}

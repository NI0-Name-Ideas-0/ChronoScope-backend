package de.ni0.chronoscope.repository;

import java.util.Collection;
import java.util.Set;

import de.ni0.chronoscope.model.Scope;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for planned dynamic-task scopes.
 */
public interface ScopeRepository extends JpaRepository<Scope, Long> {

    /**
     * Deletes all scopes belonging to the supplied dynamic task IDs.
     *
     * @param taskIds dynamic task IDs whose scopes should be removed
     * @return number of deleted scopes
     */
    long deleteByDynamicTaskIdIn(Collection<Long> taskIds);

    Set<Scope> getScopesByDynamicTaskIdentityId(Long identityId);
}

package de.ni0.chronoscope.repository;

import java.util.List;
import java.util.Set;

import de.ni0.chronoscope.model.Scope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Repository for planned dynamic-task scopes.
 */
public interface ScopeRepository extends JpaRepository<Scope, Long> {

    Set<Scope> getScopesByDynamicTaskIdentityId(Long identityId);

    Set<Scope> getScopesByDynamicTaskOrganizationIdAndDynamicTaskIdentityId(String organizationId, Long identityId);

    @Query("""
        SELECT s
        FROM Scope s
        WHERE CURRENT_TIMESTAMP BETWEEN s.startAt AND s.endAt AND s.dynamicTask.identity.id = :identityId
    """)
    List<Scope> findActiveScope(Long identityId);
}

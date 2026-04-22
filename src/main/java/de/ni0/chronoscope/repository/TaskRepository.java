package de.ni0.chronoscope.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import de.ni0.chronoscope.model.DynamicTask;
import de.ni0.chronoscope.model.Task;
 public interface TaskRepository extends JpaRepository<Task, Long> {
	@EntityGraph(attributePaths = { "labels" })
	List<Task> findByAccountIdentityId(long identityId);

	@Query("""
		select distinct t from Task t
		left join fetch t.labels
		left join fetch treat(t as DynamicTask).dependencies
		left join fetch treat(t as DynamicTask).dependents
		left join fetch treat(t as DynamicTask).scopes
		where t.account.identity.id = :identityId
		""")
	List<Task> findByAccountIdentityIdWithRelations(@Param("identityId") long identityId);

	Optional<Task> findByIdAndAccountIdentityId(Long id, Long identityId);

	@Query("select dt from DynamicTask dt join dt.dependencies dep where dep.id = :dependencyId")
	List<DynamicTask> findDynamicTasksByDependencyId(@Param("dependencyId") Long dependencyId);
}

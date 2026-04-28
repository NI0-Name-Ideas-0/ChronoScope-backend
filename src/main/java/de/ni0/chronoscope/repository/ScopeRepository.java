package de.ni0.chronoscope.repository;

import java.util.Collection;

import de.ni0.chronoscope.model.Scope;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScopeRepository extends JpaRepository<Scope, Long> {

    long deleteByDynamicTaskIdIn(Collection<Long> taskIds);
}

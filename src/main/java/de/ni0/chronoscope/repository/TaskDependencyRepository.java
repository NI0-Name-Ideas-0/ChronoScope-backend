package de.ni0.chronoscope.repository;

import de.ni0.chronoscope.model.TaskDependency;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskDependencyRepository extends JpaRepository<TaskDependency, Long> {
}

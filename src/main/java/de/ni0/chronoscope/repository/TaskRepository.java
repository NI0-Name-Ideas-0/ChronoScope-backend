package de.ni0.chronoscope.repository;

import de.ni0.chronoscope.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
}

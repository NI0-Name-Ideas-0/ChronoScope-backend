package de.ni0.chronoscope.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import de.ni0.chronoscope.model.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {
	List<Task> findByAccountIdentityId(Long identityId);
}

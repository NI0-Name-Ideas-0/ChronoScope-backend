package de.ni0.chronoscope.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import de.ni0.chronoscope.model.DynamicTask;
import de.ni0.chronoscope.model.StaticTask;
import de.ni0.chronoscope.model.Task;
import de.ni0.chronoscope.repository.TaskRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    public StaticTask createStaticTask(StaticTask task) {
        return this.taskRepository.save(task);
    }

    public DynamicTask createDynamicTask(DynamicTask task) {
        return this.taskRepository.save(task);
    }

    public List<Task> getTasksForIdentity(long identityId) {
        return Optional.ofNullable(this.taskRepository.findByAccountIdentityId(identityId))
                .orElse(Collections.emptyList());
    }

    public DynamicTask updateDynamicTask(Long id, DynamicTask task) {
        //TODO: validate task (e.g. duration > 0, minScopeDuration <= maxScopeDuration, etc.)
        return this.taskRepository.save(task);
    }

    public StaticTask updateStaticTask(Long id, StaticTask task) {
        //TODO: validate task (e.g. startAt < endAt, etc.)
        return this.taskRepository.save(task);
    }
}

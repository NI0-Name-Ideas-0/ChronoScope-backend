package de.ni0.chronoscope.service;

import java.util.ArrayList;

import org.springframework.stereotype.Service;

import de.ni0.chronoscope.model.DynamicTask;
import de.ni0.chronoscope.model.StaticTask;
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
        if (task.getDependencies() == null) {
            task.setDependencies(new ArrayList<>());
        } else {
            for (var dependency : task.getDependencies()) {
                dependency.setDynamicTask(task);
            }
        }
        return this.taskRepository.save(task);
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

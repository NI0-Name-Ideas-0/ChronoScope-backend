package de.ni0.chronoscope.service;

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
        return this.taskRepository.save(task);
    }
}

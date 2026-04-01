package de.ni0.chronoscope.service;

import de.ni0.chronoscope.model.Task;
import de.ni0.chronoscope.repository.TaskRepository;
import org.springframework.stereotype.Service;

@Service
public class TaskService {

    private TaskRepository taskRepository;

    public Task createTask(Task task) {
        return this.taskRepository.save(task);
    }

}

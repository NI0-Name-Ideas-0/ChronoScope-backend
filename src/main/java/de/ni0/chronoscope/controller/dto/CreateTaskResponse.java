package de.ni0.chronoscope.controller.dto;

import de.ni0.chronoscope.model.Task;
import lombok.Data;

@Data
public class CreateTaskResponse {
    Task createdTask;
}

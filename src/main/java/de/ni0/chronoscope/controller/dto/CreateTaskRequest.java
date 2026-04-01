package de.ni0.chronoscope.controller.dto;

import lombok.Data;

import java.time.Duration;

@Data
public class CreateTaskRequest {
    private String name;
    private Duration duration;
}

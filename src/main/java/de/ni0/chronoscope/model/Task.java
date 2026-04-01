package de.ni0.chronoscope.model;

import lombok.Data;

import java.time.Duration;

@Data
public class Task {
    private String name;
    private int complexity;
    private Duration duration;
    private int organizationId;
}

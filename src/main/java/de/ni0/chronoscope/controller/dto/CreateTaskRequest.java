package de.ni0.chronoscope.controller.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.time.Duration;

@Data
public class CreateTaskRequest {

    @NotEmpty
    @Length(min = 3, max = 32)
    private String name;
    @NotNull
    private String description;
}

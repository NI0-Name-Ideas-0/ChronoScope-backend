package de.ni0.chronoscope.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Tag {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "task_id")
    private Task task;

    private String name;
}

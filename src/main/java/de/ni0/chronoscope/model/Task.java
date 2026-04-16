package de.ni0.chronoscope.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
public class Task {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    private String name;
    private String description;
    private String rrule;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private DynamicTask dynamicTask;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private StaticTask staticTask;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Tag> tags;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaskDependency> dependencies;
}

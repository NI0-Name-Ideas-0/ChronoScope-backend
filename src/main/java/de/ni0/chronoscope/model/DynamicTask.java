package de.ni0.chronoscope.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

// @Getter/@Setter only — equals/hashCode are inherited from Task (id-based, safe with Hibernate proxies)
@Getter
@Setter
@Entity
@DiscriminatorValue("dynamic")
public class DynamicTask extends Task {

    private Integer duration;
    private Integer elapsed;
    private Integer minScopeDuration;
    private Integer maxScopeDuration;

    @OneToMany(mappedBy = "dynamicTask", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Scope> scopes;

    @OneToMany(mappedBy = "dynamicTask", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaskDependency> dependencies;
}

package de.ni0.chronoscope.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

// @Getter/@Setter only — equals/hashCode are inherited from Task (id-based, safe with Hibernate proxies)
@Getter
@Setter
@Entity
@DiscriminatorValue("dynamic")
public class DynamicTask extends Task {

    @Column(nullable = false)
    private Integer duration;
    @Column(nullable = false)
    private Integer elapsed;
    @Column(nullable = false)
    private Integer minScopeDuration;
    @Column(nullable = false)
    private Integer maxScopeDuration;

    @OneToMany(mappedBy = "dynamicTask", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Scope> scopes;

    @ManyToMany
    @JoinTable(
        name = "task_relation",
        joinColumns = @JoinColumn(name = "dependent_id"),
        inverseJoinColumns = @JoinColumn(name = "dependency_id")
    )
    private Set<DynamicTask> dependencies = new HashSet<>();

    @ManyToMany(mappedBy = "dependencies")
    private Set<DynamicTask> dependents = new HashSet<>();
}

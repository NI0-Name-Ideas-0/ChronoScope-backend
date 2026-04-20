package de.ni0.chronoscope.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// @Getter/@Setter only — equals/hashCode are inherited from Task (id-based, safe with Hibernate proxies)
@Getter
@Setter
@Entity
@DiscriminatorValue("dynamic")
@NoArgsConstructor
public class DynamicTask extends Task {

    @Column(nullable = false)
    private Duration duration;
    @Column(nullable = false)
    private Duration elapsed;
    @Column(nullable = false)
    private Duration minScopeDuration;
    @Column(nullable = false)
    private Duration maxScopeDuration;

    @OneToMany(mappedBy = "dynamicTask", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Scope> scopes;

    @OneToMany(mappedBy = "dynamicTask", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaskDependency> dependencies;

    public DynamicTask(String name, Integer difficulty, Instant startAt, Instant endAt,
                       Duration duration, Duration elapsed, Duration minScopeDuration, Duration maxScopeDuration) {
        super(name, difficulty, startAt, endAt);
        this.duration = duration;
        this.elapsed = elapsed;
        this.minScopeDuration = minScopeDuration;
        this.maxScopeDuration = maxScopeDuration;
        this.dependencies = new ArrayList<>();
    }
}

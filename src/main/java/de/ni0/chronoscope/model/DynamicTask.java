package de.ni0.chronoscope.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.util.List;

// @Getter/@Setter only — equals/hashCode are inherited from Task (id-based, safe with Hibernate proxies)
@Getter
@Setter
@Entity
@DiscriminatorValue("dynamic")
public class DynamicTask extends Task {

    @Convert(converter = DurationToLongConverter.class)
    @Column(nullable = false, columnDefinition = "BIGINT")
    private Duration duration;
    @Convert(converter = DurationToLongConverter.class)
    @Column(nullable = false, columnDefinition = "BIGINT")
    private Duration elapsed = Duration.ZERO;
    @Convert(converter = DurationToLongConverter.class)
    @Column(nullable = false, columnDefinition = "BIGINT")
    private Duration minScopeDuration;
    @Convert(converter = DurationToLongConverter.class)
    @Column(nullable = false, columnDefinition = "BIGINT")
    private Duration maxScopeDuration;

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


    @Converter
    public static class DurationToLongConverter implements AttributeConverter<Duration, Long> {

        @Override
        public Long convertToDatabaseColumn(Duration attribute) {
            return attribute == null ? null : attribute.getSeconds();
        }

        @Override
        public Duration convertToEntityAttribute(Long dbData) {
            return dbData == null ? null : Duration.ofSeconds(dbData);
        }
    }
}

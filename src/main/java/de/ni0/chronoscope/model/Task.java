package de.ni0.chronoscope.model;

import java.time.Instant;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToMany;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// @Getter/@Setter instead of @Data: @Data's generated toString/equals/hashCode are unsafe on JPA
// entities — bidirectional associations cause StackOverflowError in toString, and field-based
// hashCode becomes unstable when Hibernate assigns the id after persist.
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "dtype", discriminatorType = DiscriminatorType.STRING)
public abstract class Task {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include // id-only: stable before and after persist, works correctly with Hibernate proxies
    private Long id;

    @Column(nullable = false)
    private Long accountId;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String description;
    @Column(nullable = false)
    private Integer difficulty;
    @Column(name = "start_at", nullable = false)
    private Instant startAt;
    @Column(name = "end_at", nullable = false)
    private Instant endAt;
    @Column(nullable = false)
    private String rrule;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude // bidirectional: Label.task -> this Task, would recurse infinitely in toString
    private List<Label> labels;
}

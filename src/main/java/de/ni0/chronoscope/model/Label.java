package de.ni0.chronoscope.model;

import jakarta.persistence.*;
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
public class Label {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include // id-only: stable before and after persist, works correctly with Hibernate proxies
    private Long id;

    @ManyToOne
    @JoinColumn(name = "task_id")
    @ToString.Exclude // bidirectional: Task.labels -> this Label, would recurse infinitely in toString
    private Task task;

    private String name;
}

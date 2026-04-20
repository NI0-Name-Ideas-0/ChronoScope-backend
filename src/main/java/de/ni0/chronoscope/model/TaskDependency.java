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
public class TaskDependency {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include // id-only: stable before and after persist, works correctly with Hibernate proxies
    private Long id;

    @ManyToOne
    @JoinColumn(name = "dynamic_task_id")
    @ToString.Exclude // bidirectional: DynamicTask.dependencies -> this TaskDependency, would recurse infinitely in toString
    private DynamicTask dynamicTask;

    @ManyToOne
    @JoinColumn(name = "predecessor_dynamic_task_id")
    @ToString.Exclude // association excluded to keep toString safe and lightweight
    private DynamicTask predecessor;
}

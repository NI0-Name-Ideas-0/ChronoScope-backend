package de.ni0.chronoscope.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

// @Getter/@Setter instead of @Data: @Data's generated toString/equals/hashCode are unsafe on JPA
// entities — bidirectional associations cause StackOverflowError in toString, and field-based
// hashCode becomes unstable when Hibernate assigns the id after persist.
/**
 * Planned time window assigned to a dynamic task by the planning algorithm.
 */
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Scope {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include // id-only: stable before and after persist, works correctly with Hibernate proxies
    private Long id;

    @ManyToOne
    @JoinColumn(name = "dynamic_task_id")
    @ToString.Exclude // association excluded to keep toString safe and lightweight
    private DynamicTask dynamicTask;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;
    @Column(name = "end_at", nullable = false)
    private Instant endAt;
}

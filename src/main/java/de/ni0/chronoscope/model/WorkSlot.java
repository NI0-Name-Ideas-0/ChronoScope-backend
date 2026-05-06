package de.ni0.chronoscope.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

// @Getter/@Setter instead of @Data: @Data's generated toString/equals/hashCode are unsafe on JPA
// entities — bidirectional associations cause StackOverflowError in toString, and field-based
// hashCode becomes unstable when Hibernate assigns the id after persist.
/**
 * Availability window in which the planner may place dynamic task scopes.
 *
 * <p>Slots belong to an account and can be associated with an organizationId for later
 * planning and filtering decisions.</p>
 */
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
public class WorkSlot {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include // id-only: stable before and after persist, works correctly with Hibernate proxies
    private Long id;

    @ManyToOne
    @JoinColumn(name = "identity_id")
    @ToString.Exclude // association excluded to keep toString safe and lightweight
    private Identity identity;

    @Column(name = "organization_id", nullable = false)
    private String organizationId;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;
    @Column(name = "end_at", nullable = false)
    private Instant endAt;
}

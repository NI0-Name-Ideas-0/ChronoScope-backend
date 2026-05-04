package de.ni0.chronoscope.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

// @Getter/@Setter only — equals/hashCode are inherited from Task (id-based, safe with Hibernate proxies)
/**
 * Fixed-time task that already occupies a known interval.
 *
 * <p>Blocker static tasks can be unassigned from an organization; non-blockers must be
 * associated with one.</p>
 */
@Getter
@Setter
@Entity
@DiscriminatorValue("static")
public class StaticTask extends Task {

    @Column(nullable = false)
    private String rrule;

    @Column(nullable = false)
    private Boolean isBlocker;
}

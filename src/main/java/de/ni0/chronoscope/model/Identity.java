package de.ni0.chronoscope.model;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// @Getter/@Setter instead of @Data: @Data's generated toString/equals/hashCode are unsafe on JPA
// entities — bidirectional associations cause StackOverflowError in toString, and field-based
// hashCode becomes unstable when Hibernate assigns the id after persist.
/**
 * User identity that groups one or more linked login accounts.
 *
 * <p>The identity is the authorization boundary used by most API reads and writes, allowing
 * linked accounts to see the same task and work-slot data.</p>
 */
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
public class Identity {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include // id-only: stable before and after persist, works correctly with Hibernate proxies
    private Long id;

    @OneToMany(mappedBy = "identity")
    @ToString.Exclude // bidirectional: Account.identity -> this Identity, would recurse infinitely in toString
    private Set<Account> accounts = new HashSet<>();
}

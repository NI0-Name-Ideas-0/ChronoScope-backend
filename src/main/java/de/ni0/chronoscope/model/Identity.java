package de.ni0.chronoscope.model;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.Pattern;
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

    @Column(nullable = false, length = 10, columnDefinition = "varchar(10) default 'en_US'")
    @Pattern(regexp = "^(de_DE|en_US)$", message = "language must be one of: de_DE, en_US")
    private String language = "en_US";

    @Column(nullable = false, length = 10, columnDefinition = "varchar(10) default 'light'")
    @Pattern(regexp = "^(light|dark|system)$", message = "theme must be one of: light, dark, system")
    private String theme = "light";

    @Convert(converter = WorkSettings.WorkSettingsConverter.class)
    @Column(name = "work_settings", columnDefinition = "TEXT")
    private WorkSettings workSettings = new WorkSettings(480, Set.of("mo", "di", "mi", "do", "fr"));
}

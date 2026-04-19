package de.ni0.chronoscope.model;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
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
public class Account {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include // id-only: stable before and after persist, works correctly with Hibernate proxies
    private Long id;

    // unique subject from Keycloak
    @Column(unique = true)
    private String subject;

    @ManyToOne(optional = false)
    @JoinColumn(name = "identity_id", nullable = false)
    @ToString.Exclude // bidirectional: Identity.accounts -> this Account, would recurse infinitely in toString
    private Identity identity;

    @ManyToMany
    @JoinTable(
        name = "account_organization",
        joinColumns = @JoinColumn(name = "account_id"),
        inverseJoinColumns = @JoinColumn(name = "organization_id")
    )
    @ToString.Exclude // association excluded to keep toString safe and lightweight
    private List<Organization> organizations;
}

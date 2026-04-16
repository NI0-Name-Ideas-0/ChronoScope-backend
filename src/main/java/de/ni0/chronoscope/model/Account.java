package de.ni0.chronoscope.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
public class Account {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "identity_id")
    private Identity identity;

    @ManyToMany
    @JoinTable(
        name = "account_organization",
        joinColumns = @JoinColumn(name = "account_id"),
        inverseJoinColumns = @JoinColumn(name = "organization_id")
    )
    private List<Organization> organizations;
}

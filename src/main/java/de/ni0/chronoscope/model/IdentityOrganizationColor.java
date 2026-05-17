package de.ni0.chronoscope.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(
    uniqueConstraints = @UniqueConstraint(columnNames = {"identity_id", "organization_id"})
)
public class IdentityOrganizationColor {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "identity_id", nullable = false)
    private Identity identity;

    @Column(name = "organization_id", nullable = false)
    private String organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "color", nullable = false)
    private ColorToken color;
}

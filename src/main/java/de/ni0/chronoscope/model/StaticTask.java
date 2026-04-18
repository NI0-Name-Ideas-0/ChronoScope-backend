package de.ni0.chronoscope.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

// @Getter/@Setter only — equals/hashCode are inherited from Task (id-based, safe with Hibernate proxies)
@Getter
@Setter
@Entity
@DiscriminatorValue("static")
public class StaticTask extends Task {

    @Column(nullable = false)
    private Boolean isBlocker;
}

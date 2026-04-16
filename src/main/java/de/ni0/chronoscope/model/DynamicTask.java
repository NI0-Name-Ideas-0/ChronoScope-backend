package de.ni0.chronoscope.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

// @Getter/@Setter only — equals/hashCode are inherited from Task (id-based, safe with Hibernate proxies)
@Getter
@Setter
@Entity
@DiscriminatorValue("dynamic")
public class DynamicTask extends Task {

    private Integer difficulty;
    private Integer duration;
    private Integer elapsed;
    private Instant start;
    private Instant end;
    private Integer minScopeDuration;
    private Integer maxScopeDuration;
}

package de.ni0.chronoscope.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Data
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

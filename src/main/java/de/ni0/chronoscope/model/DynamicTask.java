package de.ni0.chronoscope.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
public class DynamicTask {

    @Id
    @GeneratedValue
    private Long id;

    private Integer difficulty;
    private Integer duration;
    private Integer elapsed;
    private Instant start;
    private Instant end;
}

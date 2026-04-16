package de.ni0.chronoscope.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Organization {

    @Id
    @GeneratedValue
    private Long id;

    private String name;
}

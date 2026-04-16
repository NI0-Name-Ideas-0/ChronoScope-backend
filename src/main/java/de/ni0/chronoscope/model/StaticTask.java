package de.ni0.chronoscope.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@DiscriminatorValue("static")
public class StaticTask extends Task {

    private Boolean blocker;
}

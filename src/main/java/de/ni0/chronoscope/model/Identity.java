package de.ni0.chronoscope.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
public class Identity {

    @Id
    @GeneratedValue
    private Long id;

    @OneToMany(mappedBy = "identity")
    private List<Account> accounts;
}

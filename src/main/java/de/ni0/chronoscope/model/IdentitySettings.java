package de.ni0.chronoscope.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.Pattern;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
public class IdentitySettings {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "identity_id", nullable = false, unique = true)
    private Identity identity;

    @Column(nullable = false, length = 10, columnDefinition = "varchar(10) default 'en_US'")
    @Pattern(regexp = "^(de_DE|en_US)$", message = "language must be one of: de_DE, en_US")
    private String language = "en_US";

    @Column(nullable = false, length = 10, columnDefinition = "varchar(10) default 'light'")
    @Pattern(regexp = "^(light|dark|system)$", message = "theme must be one of: light, dark, system")
    private String theme = "light";

    @Convert(converter = WorkSettings.WorkSettingsConverter.class)
    @Column(name = "work_settings", columnDefinition = "TEXT")
    private WorkSettings workSettings = new WorkSettings(480, java.util.Set.of("mo", "di", "mi", "do", "fr"));
}

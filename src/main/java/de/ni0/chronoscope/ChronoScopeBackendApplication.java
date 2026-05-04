package de.ni0.chronoscope;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the ChronoScope backend API.
 */
@SpringBootApplication(scanBasePackages = "de.ni0.chronoscope")
public class ChronoScopeBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChronoScopeBackendApplication.class, args);
    }

}

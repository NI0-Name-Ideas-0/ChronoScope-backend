package de.ni0.chronoscope;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "de.ni0.chronoscope")
public class ChronoScopeBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChronoScopeBackendApplication.class, args);
    }

}

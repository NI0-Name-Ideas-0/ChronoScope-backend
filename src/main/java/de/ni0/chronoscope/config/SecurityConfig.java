package de.ni0.chronoscope.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers(
                    "/test",
                    "/v1/plan", "/v1/plan/**",
                    "/v1/tasks", "/v1/tasks/**",
                    "/v1/organizationslots", "/v1/organizationslots/**").permitAll()
            .anyRequest().authenticated()
        );
        http.csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }
}

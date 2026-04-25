package de.ni0.chronoscope.config;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class WebConfig {

    private static final List<String> ALLOWED_METHODS = List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
    private static final List<String> ALLOWED_HEADERS = List.of(CorsConfiguration.ALL);
    private static final long MAX_AGE_SECONDS = 3600L;

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        var source = new UrlBasedCorsConfigurationSource();
        var allowedOrigins = corsProperties.allowedOrigins();

        if (!allowedOrigins.isEmpty()) {
            var configuration = new CorsConfiguration();
            // If the allowed origins contain "*", we need to use allowed origin patterns to allow credentials
            if (allowedOrigins.contains(CorsConfiguration.ALL)) {
                configuration.setAllowedOriginPatterns(List.of(CorsConfiguration.ALL));
            } else {
                configuration.setAllowedOrigins(allowedOrigins);
            }
            configuration.setAllowedMethods(ALLOWED_METHODS);
            configuration.setAllowedHeaders(ALLOWED_HEADERS);
            configuration.setAllowCredentials(true);
            configuration.setMaxAge(MAX_AGE_SECONDS);

            source.registerCorsConfiguration("/**", configuration);
        }

        return source;
    }
}

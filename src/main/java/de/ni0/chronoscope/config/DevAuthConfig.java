package de.ni0.chronoscope.config;

import java.util.List;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * Development-profile authentication configuration that accepts a local static bearer token.
 */
@Configuration
@Profile("dev & !prod")
@EnableConfigurationProperties(DevAuthProperties.class)
public class DevAuthConfig {

    /**
     * Creates a JWT decoder that handles the configured local token before delegating to Keycloak.
     *
     * @param devAuthProperties local authentication settings
     * @param issuerUri configured OIDC issuer URI
     * @return JWT decoder for development
     */
    @Bean
    public JwtDecoder jwtDecoder(
        DevAuthProperties devAuthProperties,
        @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri
    ) {
        return new DevLocalJwtDecoder(
            devAuthProperties,
            () -> JwtDecoders.fromIssuerLocation(issuerUri)
        );
    }
}

/**
 * JWT decoder that short-circuits the configured development token and lazily initializes the real decoder.
 */
final class DevLocalJwtDecoder implements JwtDecoder {

    private final DevAuthProperties properties;
    private final Supplier<JwtDecoder> delegateSupplier;
    private volatile JwtDecoder delegate;

    DevLocalJwtDecoder(DevAuthProperties properties, Supplier<JwtDecoder> delegateSupplier) {
        this.properties = properties;
        this.delegateSupplier = delegateSupplier;
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        if (properties.token().equals(token)) {
            return Jwt.withTokenValue(token)
                .header("alg", "none")
                .claim("sub", properties.subject())
                .claim("email", properties.mail())
                .claim("organizationId", List.copyOf(properties.organizations()))
                .claim("groups", List.copyOf(properties.groups()))
                .build();
        }

        return delegate().decode(token);
    }

    private JwtDecoder delegate() {
        JwtDecoder current = delegate;
        if (current == null) {
            synchronized (this) {
                current = delegate;
                if (current == null) {
                    current = delegateSupplier.get();
                    delegate = current;
                }
            }
        }
        return current;
    }
}

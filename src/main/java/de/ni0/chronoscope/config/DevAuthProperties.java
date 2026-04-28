package de.ni0.chronoscope.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chronoscope.dev-auth")
public record DevAuthProperties(
    String token,
    String subject,
    List<String> organizations
) {

    public static final String DEFAULT_TOKEN = "local-test-user";
    public static final String DEFAULT_SUBJECT = "local-test-user";
    public static final List<String> DEFAULT_ORGANIZATIONS = List.of("private", "chronoscope-local");

    public DevAuthProperties {
        token = normalize(token, DEFAULT_TOKEN);
        subject = normalize(subject, DEFAULT_SUBJECT);
        organizations = organizations == null || organizations.isEmpty()
            ? DEFAULT_ORGANIZATIONS
            : organizations.stream()
                .map(String::trim)
                .filter(organization -> !organization.isBlank())
                .toList();

        if (organizations.isEmpty()) {
            organizations = DEFAULT_ORGANIZATIONS;
        }
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}

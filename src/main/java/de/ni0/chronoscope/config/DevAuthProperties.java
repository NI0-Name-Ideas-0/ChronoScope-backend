package de.ni0.chronoscope.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chronoscope.dev-auth")
public record DevAuthProperties(
    String token,
    String subject,
    String mail,
    List<String> organizations,
    List<String> groups
) {

    public static final String DEFAULT_TOKEN = "local-test-user";
    public static final String DEFAULT_SUBJECT = "local-test-user";
    public static final List<String> DEFAULT_ORGANIZATIONS = List.of("private", "chronoscope-local");
    public static final List<String> DEFAULT_GROUPS = List.of();

    public DevAuthProperties(String token, String subject, List<String> organizations) {
        this(token, subject, "", organizations, DEFAULT_GROUPS);
    }

    public DevAuthProperties {
        token = normalize(token, DEFAULT_TOKEN);
        subject = normalize(subject, DEFAULT_SUBJECT);
        mail = mail == null ? "" : mail.trim();
        organizations = normalizeList(organizations);

        if (organizations.isEmpty()) {
            organizations = DEFAULT_ORGANIZATIONS;
        }

        groups = normalizeList(groups);
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static List<String> normalizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        return values.stream()
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .toList();
    }
}

package de.ni0.chronoscope.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.SecurityScheme;

class OpenApiConfigTest {

    @Test
    void chronoScopeOpenAPI_ReturnsConfiguredOAuth2SecurityScheme() {
        OpenApiConfig config = new OpenApiConfig();

        OpenAPI openAPI = config.chronoScopeOpenAPI();

        assertNotNull(openAPI);
        assertEquals("ChronoScope API", openAPI.getInfo().getTitle());
        assertEquals("https://chronoscope.ni0.team/api", openAPI.getServers().get(0).getUrl());

        SecurityScheme oauth2Scheme = (SecurityScheme) openAPI.getComponents().getSecuritySchemes().get("oauth2");
        assertEquals(SecurityScheme.Type.OAUTH2, oauth2Scheme.getType());

        OAuthFlow authorizationCode = oauth2Scheme.getFlows().getAuthorizationCode();
        assertEquals("https://auth.ni0.team/realms/ni0/protocol/openid-connect/auth", authorizationCode.getAuthorizationUrl());
        assertEquals("https://auth.ni0.team/realms/ni0/protocol/openid-connect/token", authorizationCode.getTokenUrl());
        assertEquals("Read access", authorizationCode.getScopes().get("read"));
        assertEquals("Write access", authorizationCode.getScopes().get("write"));
    }
}

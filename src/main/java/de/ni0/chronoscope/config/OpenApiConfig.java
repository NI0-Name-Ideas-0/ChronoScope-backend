package de.ni0.chronoscope.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String KEYCLOAK_BASE = "https://auth.ni0.team/realms/ni0/protocol/openid-connect";

    @Bean
    public OpenAPI chronoScopeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ChronoScope API")
                        .description("Online task planning system — manage tasks, work slots, dependencies and generate optimized plans.")
                        .version("1.1.0"))
                .addSecurityItem(new SecurityRequirement().addList("oauth2"))
                .components(new Components()
                        .addSecuritySchemes("oauth2", new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .description("Keycloak OAuth2 (Authorization Code flow)")
                                .flows(new OAuthFlows()
                                        .authorizationCode(new OAuthFlow()
                                                .authorizationUrl(KEYCLOAK_BASE + "/auth")
                                                .tokenUrl(KEYCLOAK_BASE + "/token")
                                                .scopes(new Scopes()
                                                        .addString("read", "Read access")
                                                        .addString("write", "Write access"))))));
    }
}

package de.ni0.chronoscope.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import de.ni0.chronoscope.model.Identity;
import de.ni0.chronoscope.service.KeycloakService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IdentityControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KeycloakService keycloakService;

    @Test
    void getIdentity_ReturnsAdminOrganizationsFromKeycloak() throws Exception {
        String subject = "it-identity-subject-" + System.nanoTime();
        when(keycloakService.getAdminOrganizations(any(Identity.class)))
            .thenReturn(Set.of("dhbw-stuttgart", "chronoscope-local"));

        mockMvc.perform(get("/v1/identity")
                .with(jwt().jwt(jwt -> jwt
                    .subject(subject))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.adminOrganizations.length()").value(2))
            .andExpect(jsonPath("$.adminOrganizations", hasItem("dhbw-stuttgart")))
            .andExpect(jsonPath("$.adminOrganizations", hasItem("chronoscope-local")));
    }
}

package de.ni0.chronoscope.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IdentityControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getIdentity_ReturnsAdminOrganizationsFromGroupsClaim() throws Exception {
        String subject = "it-identity-subject-" + System.nanoTime();

        mockMvc.perform(get("/v1/identity")
                .with(jwt().jwt(jwt -> jwt
                    .subject(subject)
                    .claim("organization", List.of("private"))
                    .claim("groups", List.of(
                        "/org-admin/dhbw-stuttgart",
                        "/users/dhbw-stuttgart",
                        "/org-admin/chronoscope-local/nested"
                    )))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.adminOrganizations.length()").value(2))
            .andExpect(jsonPath("$.adminOrganizations", hasItem("dhbw-stuttgart")))
            .andExpect(jsonPath("$.adminOrganizations", hasItem("chronoscope-local")));
    }
}

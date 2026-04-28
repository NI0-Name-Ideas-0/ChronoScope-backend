package de.ni0.chronoscope.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfigurationSource;

@SpringBootTest(properties = "cors.allowed-origins[0]=*")
@AutoConfigureMockMvc
class CorsConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Test
    void preflightRequestUsesConfiguredAllowedOrigin() throws Exception {
        mockMvc.perform(options("/v1/tasks")
                        .header(HttpHeaders.ORIGIN, "https://chronoscope.ni0.team")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://chronoscope.ni0.team"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void corsConfigurationIsRestrictedToPublicApiPaths() {
        assertNotNull(corsConfigurationSource.getCorsConfiguration(
                new MockHttpServletRequest("OPTIONS", "/v1/tasks")));
        assertNull(corsConfigurationSource.getCorsConfiguration(
                new MockHttpServletRequest("OPTIONS", "/v3/api-docs.yaml")));
    }
}

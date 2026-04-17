package de.ni0.chronoscope.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TestControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testEndpoint_ReturnsExpectedString() throws Exception {
        mockMvc.perform(get("/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello from ChronoScope! Test Test"));
    }

    @Test
    void identityEndpoint_ReturnsNotImplemented() throws Exception {
        mockMvc.perform(get("/v1/identity"))
                .andExpect(status().isNotImplemented())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:chronoscope:error:api-not-implemented"))
                .andExpect(jsonPath("$.title").value("Not Implemented"))
                .andExpect(jsonPath("$.status").value(501))
                .andExpect(jsonPath("$.detail").value("Not implemented yet"))
                .andExpect(jsonPath("$.instance").value("/v1/identity"))
                .andExpect(jsonPath("$.errorCode").value("API_NOT_IMPLEMENTED"));
    }

    @Test
    void accountLinkValidation_ReturnsProblemDetail() throws Exception {
        mockMvc.perform(post("/v1/identity/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetEmail\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:chronoscope:error:validation-error"))
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.instance").value("/v1/identity/accounts"))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("targetEmail")));
    }

    @Test
    void unknownPublicApiRoute_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/v1/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:chronoscope:error:resource-not-found"))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("The requested resource was not found"))
                .andExpect(jsonPath("$.instance").value("/v1/does-not-exist"))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }
}

package de.ni0.chronoscope.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getTasks_ReturnsNotImplemented() throws Exception {
        mockMvc.perform(get("/v1/tasks"))
            .andExpect(status().isNotImplemented())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("urn:chronoscope:error:api-not-implemented"))
            .andExpect(jsonPath("$.title").value("Not Implemented"))
            .andExpect(jsonPath("$.status").value(501))
            .andExpect(jsonPath("$.detail").value("Not implemented yet"))
            .andExpect(jsonPath("$.instance").value("/v1/tasks"))
            .andExpect(jsonPath("$.errorCode").value("API_NOT_IMPLEMENTED"));
    }
}
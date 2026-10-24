package com.devflow.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Actuator Health & Observability Integration Test")
@AutoConfigureMockMvc
class ActuatorIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("should report UP health status and healthy PostgreSQL database connectivity")
    void shouldReportUpHealthStatusWithDatabase() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.components.db.status", is("UP")))
                .andExpect(jsonPath("$.components.db.details.database", is("PostgreSQL")))
                .andExpect(header().exists("X-Request-ID"));
    }

    @Test
    @DisplayName("should return safe application metadata from /actuator/info without leaking secrets")
    void shouldReturnSafeInfoMetadata() throws Exception {
        mockMvc.perform(get("/actuator/info")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app.name").exists())
                .andExpect(jsonPath("$.app.version").exists())
                .andExpect(jsonPath("$.app.description").exists())
                // Ensure no secrets or sensitive configuration leaked
                .andExpect(jsonPath("$.jwt").doesNotExist())
                .andExpect(jsonPath("$.db").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.secret").doesNotExist())
                .andExpect(header().exists("X-Request-ID"));
    }

    @Test
    @DisplayName("should not expose sensitive actuator endpoints such as /actuator/env")
    void shouldNotExposeSensitiveActuatorEndpoints() throws Exception {
        // /actuator/env is not in management.endpoints.web.exposure.include, returning 404 or 401/403
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(get("/actuator/beans"))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(get("/actuator/heapdump"))
                .andExpect(status().is4xxClientError());
    }
}

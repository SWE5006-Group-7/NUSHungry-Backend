package com.nushungry.configserver.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security configuration tests
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("native")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointShouldBePublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void configEndpointShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/application/default"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void configEndpointShouldAcceptValidCredentials() throws Exception {
        mockMvc.perform(get("/application/default")
                        .with(httpBasic("config", "config123")))
                .andExpect(status().isOk());
    }

    @Test
    void configEndpointShouldRejectInvalidCredentials() throws Exception {
        mockMvc.perform(get("/application/default")
                        .with(httpBasic("wrong", "wrong")))
                .andExpect(status().isUnauthorized());
    }
}

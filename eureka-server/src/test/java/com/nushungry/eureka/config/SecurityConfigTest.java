package com.nushungry.eureka.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for Security Configuration
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "eureka.client.register-with-eureka=false",
    "eureka.client.fetch-registry=false"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointIsPublic() throws Exception {
        // Health endpoint should be accessible without authentication
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk());
    }

    @Test
    void infoEndpointIsPublic() throws Exception {
        // Info endpoint should be accessible without authentication
        mockMvc.perform(get("/actuator/info"))
            .andExpect(status().isOk());
    }

    @Test
    void dashboardRequiresAuthentication() throws Exception {
        // Dashboard should require authentication
        mockMvc.perform(get("/"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "eureka", roles = "ADMIN")
    void authenticatedUserCanAccessDashboard() throws Exception {
        // Authenticated user should be able to access dashboard
        mockMvc.perform(get("/"))
            .andExpect(status().isOk());
    }
}

package com.nushungry.adminservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminAuthController.class)
class AdminAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    void shouldReturnAuthenticatedMessageWhenUserIsAuthenticated() throws Exception {
        mockMvc.perform(get("/api/admin/auth/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("认证成功"))
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    void shouldReturnUnauthenticatedMessageWhenUserIsNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/admin/auth/test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"ROLE_USER"})
    void shouldAllowAccessWithNonAdminUser() throws Exception {
        mockMvc.perform(get("/api/admin/auth/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("认证成功"))
                .andExpect(jsonPath("$.username").value("testuser"));
    }
}

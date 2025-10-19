package com.nushungry.adminservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nushungry.adminservice.dto.*;
import com.nushungry.adminservice.service.UserServiceClient;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminUserController.class)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserServiceClient userServiceClient;

    private UserDTO mockUser;
    private UserListResponse mockUserListResponse;

    @BeforeEach
    void setUp() {
        mockUser = UserDTO.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .role("USER")
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();

        mockUserListResponse = UserListResponse.builder()
                .users(List.of(mockUser))
                .totalItems(1)
                .totalPages(1)
                .currentPage(0)
                .pageSize(10)
                .build();
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    void shouldReturnUserListSuccessfully() throws Exception {
        when(userServiceClient.getUserList(0, 10, "createdAt", "DESC", null))
                .thenReturn(mockUserListResponse);

        mockMvc.perform(get("/api/admin/users")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "createdAt")
                        .param("sortDirection", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[0].username").value("testuser"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    void shouldReturnUserListWithSearchParam() throws Exception {
        when(userServiceClient.getUserList(0, 10, "createdAt", "DESC", "test"))
                .thenReturn(mockUserListResponse);

        mockMvc.perform(get("/api/admin/users")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "createdAt")
                        .param("sortDirection", "DESC")
                        .param("search", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[0].username").value("testuser"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    void shouldReturnUserDetailById() throws Exception {
        when(userServiceClient.getUserById(1L)).thenReturn(mockUser);

        mockMvc.perform(get("/api/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    void shouldCreateUserSuccessfully() throws Exception {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("newuser")
                .email("new@example.com")
                .password("password123")
                .role("USER")
                .build();

        when(userServiceClient.createUser(any(CreateUserRequest.class))).thenReturn(mockUser);

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("用户创建成功"))
                .andExpect(jsonPath("$.user.username").value("testuser"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    void shouldUpdateUserSuccessfully() throws Exception {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .username("updateduser")
                .email("updated@example.com")
                .build();

        when(userServiceClient.updateUser(eq(1L), any(UpdateUserRequest.class))).thenReturn(mockUser);

        mockMvc.perform(put("/api/admin/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("用户更新成功"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    void shouldDeleteUserSuccessfully() throws Exception {
        doNothing().when(userServiceClient).deleteUser(1L);

        mockMvc.perform(delete("/api/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("用户删除成功"));

        verify(userServiceClient, times(1)).deleteUser(1L);
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    void shouldUpdateUserStatusSuccessfully() throws Exception {
        Map<String, Boolean> statusRequest = Map.of("enabled", false);
        when(userServiceClient.updateUserStatus(1L, false)).thenReturn(mockUser);

        mockMvc.perform(patch("/api/admin/users/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("账户已禁用"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    void shouldReturnBadRequestWhenEnabledParamMissing() throws Exception {
        Map<String, String> invalidRequest = Map.of("invalid", "param");

        mockMvc.perform(patch("/api/admin/users/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("缺少enabled参数"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    void shouldUpdateUserRoleSuccessfully() throws Exception {
        Map<String, String> roleRequest = Map.of("role", "ADMIN");
        when(userServiceClient.updateUserRole(1L, "ADMIN")).thenReturn(mockUser);

        mockMvc.perform(put("/api/admin/users/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("角色修改成功"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    void shouldReturnBadRequestWhenRoleParamMissing() throws Exception {
        Map<String, String> invalidRequest = Map.of("invalid", "param");

        mockMvc.perform(put("/api/admin/users/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("缺少role参数"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    void shouldResetUserPasswordSuccessfully() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .password("newpassword123")
                .build();

        doNothing().when(userServiceClient).resetUserPassword(1L, "newpassword123");

        mockMvc.perform(post("/api/admin/users/1/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("密码重置成功"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    void shouldPerformBatchOperationSuccessfully() throws Exception {
        BatchOperationRequest request = BatchOperationRequest.builder()
                .userIds(List.of(1L, 2L, 3L))
                .operation("ENABLE")
                .build();

        when(userServiceClient.batchOperation(any(BatchOperationRequest.class))).thenReturn(3);

        mockMvc.perform(post("/api/admin/users/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("批量操作成功，影响 3 个用户"))
                .andExpect(jsonPath("$.affectedCount").value(3));
    }

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user", authorities = {"ROLE_USER"})
    void shouldReturn403WhenNotAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    void shouldHandleExceptionWhenServiceFails() throws Exception {
        when(userServiceClient.getUserList(anyInt(), anyInt(), anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("获取用户列表失败"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    void shouldHandleExceptionWhenDeletingUser() throws Exception {
        doThrow(new RuntimeException("Delete failed")).when(userServiceClient).deleteUser(1L);

        mockMvc.perform(delete("/api/admin/users/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("删除用户失败"));
    }
}

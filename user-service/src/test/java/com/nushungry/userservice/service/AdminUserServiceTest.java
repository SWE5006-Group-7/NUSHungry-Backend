package com.nushungry.userservice.service;

import com.nushungry.userservice.dto.*;
import com.nushungry.userservice.model.User;
import com.nushungry.userservice.model.UserRole;
import com.nushungry.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AdminUserService 单元测试
 * 使用轻量级 Mockito 测试,不启动 Spring 容器
 */
@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminUserService adminUserService;

    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole(UserRole.ROLE_USER);
        testUser.setEnabled(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        // 创建管理员用户
        adminUser = new User();
        adminUser.setId(2L);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword("encodedPassword");
        adminUser.setRole(UserRole.ROLE_ADMIN);
        adminUser.setEnabled(true);
        adminUser.setCreatedAt(LocalDateTime.now());
    }

    // ==================== getUserList() 测试 ====================

    @Test
    void testGetUserList_Success() {
        // Arrange
        List<User> users = Arrays.asList(testUser, adminUser);
        Page<User> userPage = new PageImpl<>(users);
        when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);

        // Act
        UserListResponse response = adminUserService.getUserList(0, 10, "id", "ASC", null);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getUsers().size());
        assertEquals(0, response.getCurrentPage());
        assertEquals(2, response.getTotalItems());
        assertEquals(1, response.getTotalPages());
    }

    @Test
    void testGetUserList_WithSearch() {
        // Arrange
        Page<User> userPage = new PageImpl<>(Arrays.asList(testUser));
        when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);

        // Act
        UserListResponse response = adminUserService.getUserList(0, 10, "id", "DESC", "test");

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getUsers().size());
    }

    // ==================== getUserById() 测试 ====================

    @Test
    void testGetUserById_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        UserDTO result = adminUserService.getUserById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void testGetUserById_NotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            adminUserService.getUserById(999L);
        });

        assertTrue(exception.getMessage().contains("用户不存在"));
    }

    // ==================== createUser() 测试 ====================

    @Test
    void testCreateUser_Success() {
        // Arrange
        CreateUserRequest request = CreateUserRequest.builder()
                .username("newuser")
                .email("newuser@test.com")
                .password("Password123")
                .role("ROLE_USER")
                .enabled(true)
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@test.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        UserDTO result = adminUserService.createUser(request);

        // Assert
        assertNotNull(result);
        verify(passwordEncoder).encode("Password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testCreateUser_UsernameExists() {
        // Arrange
        CreateUserRequest request = CreateUserRequest.builder()
                .username("existinguser")
                .email("new@test.com")
                .password("Password123")
                .build();

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            adminUserService.createUser(request);
        });

        assertTrue(exception.getMessage().contains("用户名已存在"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testCreateUser_EmailExists() {
        // Arrange
        CreateUserRequest request = CreateUserRequest.builder()
                .username("newuser")
                .email("existing@test.com")
                .password("Password123")
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            adminUserService.createUser(request);
        });

        assertTrue(exception.getMessage().contains("邮箱已存在"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testCreateUser_InvalidRole() {
        // Arrange
        CreateUserRequest request = CreateUserRequest.builder()
                .username("newuser")
                .email("newuser@test.com")
                .password("Password123")
                .role("INVALID_ROLE")
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@test.com")).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            adminUserService.createUser(request);
        });

        assertTrue(exception.getMessage().contains("无效的角色"));
    }

    // ==================== updateUser() 测试 ====================

    @Test
    void testUpdateUser_Success() {
        // Arrange
        UpdateUserRequest request = UpdateUserRequest.builder()
                .username("updateduser")
                .email("updated@test.com")
                .role("ROLE_ADMIN")
                .enabled(false)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("updateduser")).thenReturn(false);
        when(userRepository.existsByEmail("updated@test.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        UserDTO result = adminUserService.updateUser(1L, request);

        // Assert
        assertNotNull(result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testUpdateUser_UserNotFound() {
        // Arrange
        UpdateUserRequest request = UpdateUserRequest.builder().build();
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            adminUserService.updateUser(999L, request);
        });

        assertTrue(exception.getMessage().contains("用户不存在"));
    }

    @Test
    void testUpdateUser_UsernameAlreadyExists() {
        // Arrange
        UpdateUserRequest request = UpdateUserRequest.builder()
                .username("existinguser")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            adminUserService.updateUser(1L, request);
        });

        assertTrue(exception.getMessage().contains("用户名已存在"));
    }

    // ==================== deleteUser() 测试 ====================

    @Test
    void testDeleteUser_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        adminUserService.deleteUser(1L);

        // Assert
        verify(userRepository).delete(testUser);
    }

    @Test
    void testDeleteUser_UserNotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            adminUserService.deleteUser(999L);
        });

        assertTrue(exception.getMessage().contains("用户不存在"));
        verify(userRepository, never()).delete(any(User.class));
    }

    // ==================== updateUserStatus() 测试 ====================

    @Test
    void testUpdateUserStatus_Enable() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        UserDTO result = adminUserService.updateUserStatus(1L, true);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertTrue(userCaptor.getValue().getEnabled());
    }

    @Test
    void testUpdateUserStatus_Disable() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        UserDTO result = adminUserService.updateUserStatus(1L, false);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertFalse(userCaptor.getValue().getEnabled());
    }

    // ==================== updateUserRole() 测试 ====================

    @Test
    void testUpdateUserRole_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        UserDTO result = adminUserService.updateUserRole(1L, "ROLE_ADMIN");

        // Assert
        assertNotNull(result);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(UserRole.ROLE_ADMIN, userCaptor.getValue().getRole());
    }

    @Test
    void testUpdateUserRole_InvalidRole() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            adminUserService.updateUserRole(1L, "INVALID_ROLE");
        });

        assertTrue(exception.getMessage().contains("无效的角色"));
    }

    // ==================== resetUserPassword() 测试 ====================

    @Test
    void testResetUserPassword_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("NewPassword123")).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        adminUserService.resetUserPassword(1L, "NewPassword123");

        // Assert
        verify(passwordEncoder).encode("NewPassword123");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("newEncodedPassword", userCaptor.getValue().getPassword());
    }

    // ==================== batchOperation() 测试 ====================

    @Test
    void testBatchOperation_Enable() {
        // Arrange
        BatchOperationRequest request = BatchOperationRequest.builder()
                .userIds(Arrays.asList(1L, 2L))
                .operation("enable")
                .build();

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        int result = adminUserService.batchOperation(request);

        // Assert
        assertEquals(2, result);
        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    void testBatchOperation_Disable() {
        // Arrange
        BatchOperationRequest request = BatchOperationRequest.builder()
                .userIds(Arrays.asList(1L, 2L))
                .operation("disable")
                .build();

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        int result = adminUserService.batchOperation(request);

        // Assert
        assertEquals(2, result);
    }

    @Test
    void testBatchOperation_Delete() {
        // Arrange
        BatchOperationRequest request = BatchOperationRequest.builder()
                .userIds(Arrays.asList(1L, 2L))
                .operation("delete")
                .build();

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));

        // Act
        int result = adminUserService.batchOperation(request);

        // Assert
        assertEquals(2, result);
        verify(userRepository, times(2)).delete(any(User.class));
    }

    @Test
    void testBatchOperation_EmptyList() {
        // Arrange
        BatchOperationRequest request = BatchOperationRequest.builder()
                .userIds(Arrays.asList())
                .operation("enable")
                .build();

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            adminUserService.batchOperation(request);
        });

        assertTrue(exception.getMessage().contains("用户ID列表不能为空"));
    }

    @Test
    void testBatchOperation_InvalidOperation() {
        // Arrange
        BatchOperationRequest request = BatchOperationRequest.builder()
                .userIds(Arrays.asList(1L, 2L))
                .operation("invalid_operation")
                .build();

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            adminUserService.batchOperation(request);
        });

        assertTrue(exception.getMessage().contains("无效的操作类型"));
    }

    @Test
    void testBatchOperation_PartialFailure() {
        // Arrange
        BatchOperationRequest request = BatchOperationRequest.builder()
                .userIds(Arrays.asList(1L, 999L))
                .operation("enable")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        int result = adminUserService.batchOperation(request);

        // Assert
        assertEquals(1, result); // 只有一个成功
        verify(userRepository, times(1)).save(any(User.class));
    }

    // ==================== getLatestUsers() 测试 ====================

    @Test
    void testGetLatestUsers_Success() {
        // Arrange
        Page<User> userPage = new PageImpl<>(Arrays.asList(testUser, adminUser));
        when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);

        // Act
        List<UserDTO> result = adminUserService.getLatestUsers(10);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(userRepository).findAll(any(Pageable.class));
    }

    // ==================== changeUserPassword() 测试 ====================

    @Test
    void testChangeUserPassword_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("NewPassword123")).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        adminUserService.changeUserPassword(1L, "NewPassword123");

        // Assert
        verify(passwordEncoder).encode("NewPassword123");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("newEncodedPassword", userCaptor.getValue().getPassword());
    }

    @Test
    void testChangeUserPassword_UserNotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            adminUserService.changeUserPassword(999L, "NewPassword123");
        });

        assertTrue(exception.getMessage().contains("用户不存在"));
        verify(passwordEncoder, never()).encode(anyString());
    }
}

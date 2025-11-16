package com.nushungry.userservice.service;

import com.nushungry.userservice.dto.AdminLoginRequest;
import com.nushungry.userservice.dto.AdminLoginResponse;
import com.nushungry.userservice.dto.TokenVerifyResponse;
import com.nushungry.userservice.model.User;
import com.nushungry.userservice.model.UserRole;
import com.nushungry.userservice.repository.UserRepository;
import com.nushungry.userservice.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AdminAuthService 单元测试
 * 使用轻量级 Mockito 测试,不启动 Spring 容器
 */
@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AdminAuthService adminAuthService;

    private User adminUser;
    private User normalUser;
    private AdminLoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        // 创建管理员用户
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@test.com");
        adminUser.setPassword("encodedPassword");
        adminUser.setRole(UserRole.ROLE_ADMIN);
        adminUser.setEnabled(true);
        adminUser.setCreatedAt(LocalDateTime.now());

        // 创建普通用户
        normalUser = new User();
        normalUser.setId(2L);
        normalUser.setUsername("user");
        normalUser.setEmail("user@test.com");
        normalUser.setPassword("encodedPassword");
        normalUser.setRole(UserRole.ROLE_USER);
        normalUser.setEnabled(true);

        // 创建登录请求
        loginRequest = new AdminLoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("Admin@123");
    }

    // ==================== adminLogin() 测试 ====================

    @Test
    void testAdminLogin_Success() {
        // Arrange
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("Admin@123", "encodedPassword")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(adminUser);
        when(jwtUtil.generateToken(anyString(), anyMap())).thenReturn("adminToken123");
        when(jwtUtil.getExpiration()).thenReturn(3600000L);

        // Act
        AdminLoginResponse response = adminAuthService.adminLogin(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals("adminToken123", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600000L, response.getExpiresIn());
        assertEquals("admin", response.getUsername());
        assertEquals("admin@test.com", response.getEmail());
        assertEquals("ADMIN", response.getRole());
        assertTrue(response.isAdmin());

        // 验证密码检查
        verify(passwordEncoder).matches("Admin@123", "encodedPassword");

        // 验证用户保存(更新最后登录时间)
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertNotNull(userCaptor.getValue().getLastLogin());
    }

    @Test
    void testAdminLogin_UserNotFound() {
        // Arrange
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());
        loginRequest.setUsername("nonexistent");

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            adminAuthService.adminLogin(loginRequest);
        });

        assertEquals("用户名或密码错误", exception.getMessage());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testAdminLogin_InvalidPassword() {
        // Arrange
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("WrongPassword", "encodedPassword")).thenReturn(false);
        loginRequest.setPassword("WrongPassword");

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            adminAuthService.adminLogin(loginRequest);
        });

        assertEquals("用户名或密码错误", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testAdminLogin_NotAdminUser() {
        // Arrange
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(normalUser));
        when(passwordEncoder.matches("Admin@123", "encodedPassword")).thenReturn(true);
        loginRequest.setUsername("user");

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            adminAuthService.adminLogin(loginRequest);
        });

        assertEquals("无管理员权限", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testAdminLogin_DisabledAccount() {
        // Arrange
        adminUser.setEnabled(false);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("Admin@123", "encodedPassword")).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            adminAuthService.adminLogin(loginRequest);
        });

        assertEquals("账户已被禁用", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    // ==================== refreshToken() 测试 ====================

    @Test
    void testRefreshToken_Success() {
        // Arrange
        String oldToken = "oldToken123";
        when(jwtUtil.validateToken(oldToken)).thenReturn(true);
        when(jwtUtil.isTokenExpired(oldToken)).thenReturn(false);
        when(jwtUtil.getUsernameFromToken(oldToken)).thenReturn("admin");
        when(jwtUtil.getUserIdFromToken(oldToken)).thenReturn(1L);
        when(jwtUtil.getRoleFromToken(oldToken)).thenReturn("ROLE_ADMIN");
        when(jwtUtil.generateToken(anyString(), anyMap())).thenReturn("newToken456");
        when(jwtUtil.getExpiration()).thenReturn(3600000L);

        // Act
        AdminLoginResponse response = adminAuthService.refreshToken(oldToken);

        // Assert
        assertNotNull(response);
        assertEquals("newToken456", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600000L, response.getExpiresIn());
        assertEquals("admin", response.getUsername());
        assertEquals("ROLE_ADMIN", response.getRole());
        assertTrue(response.isAdmin());
    }

    @Test
    void testRefreshToken_InvalidToken() {
        // Arrange
        String invalidToken = "invalidToken";
        when(jwtUtil.validateToken(invalidToken)).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            adminAuthService.refreshToken(invalidToken);
        });

        assertTrue(exception.getMessage().contains("Token无效"));
    }

    @Test
    void testRefreshToken_ExpiredToken() {
        // Arrange
        String expiredToken = "expiredToken";
        when(jwtUtil.validateToken(expiredToken)).thenReturn(true);
        when(jwtUtil.isTokenExpired(expiredToken)).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            adminAuthService.refreshToken(expiredToken);
        });

        assertTrue(exception.getMessage().contains("Token已过期"));
    }

    @Test
    void testRefreshToken_NotAdminRole() {
        // Arrange
        String userToken = "userToken";
        when(jwtUtil.validateToken(userToken)).thenReturn(true);
        when(jwtUtil.isTokenExpired(userToken)).thenReturn(false);
        when(jwtUtil.getUsernameFromToken(userToken)).thenReturn("user");
        when(jwtUtil.getUserIdFromToken(userToken)).thenReturn(2L);
        when(jwtUtil.getRoleFromToken(userToken)).thenReturn("ROLE_USER");

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            adminAuthService.refreshToken(userToken);
        });

        assertTrue(exception.getMessage().contains("无管理员权限"));
    }

    // ==================== verifyToken() 测试 ====================

    @Test
    void testVerifyToken_Success() {
        // Arrange
        String validToken = "validAdminToken";
        when(jwtUtil.validateToken(validToken)).thenReturn(true);
        when(jwtUtil.isTokenExpired(validToken)).thenReturn(false);
        when(jwtUtil.getUsernameFromToken(validToken)).thenReturn("admin");
        when(jwtUtil.getRoleFromToken(validToken)).thenReturn("ROLE_ADMIN");

        // Act
        TokenVerifyResponse response = adminAuthService.verifyToken(validToken);

        // Assert
        assertNotNull(response);
        assertTrue(response.isValid());
        assertEquals("admin", response.getUsername());
        assertEquals("ROLE_ADMIN", response.getRole());
        assertNull(response.getReason());
    }

    @Test
    void testVerifyToken_InvalidToken() {
        // Arrange
        String invalidToken = "invalidToken";
        when(jwtUtil.validateToken(invalidToken)).thenReturn(false);

        // Act
        TokenVerifyResponse response = adminAuthService.verifyToken(invalidToken);

        // Assert
        assertNotNull(response);
        assertFalse(response.isValid());
        assertEquals("Invalid token", response.getReason());
    }

    @Test
    void testVerifyToken_ExpiredToken() {
        // Arrange
        String expiredToken = "expiredToken";
        when(jwtUtil.validateToken(expiredToken)).thenReturn(true);
        when(jwtUtil.isTokenExpired(expiredToken)).thenReturn(true);

        // Act
        TokenVerifyResponse response = adminAuthService.verifyToken(expiredToken);

        // Assert
        assertNotNull(response);
        assertFalse(response.isValid());
        assertEquals("Token expired", response.getReason());
    }

    @Test
    void testVerifyToken_NotAdminRole() {
        // Arrange
        String userToken = "userToken";
        when(jwtUtil.validateToken(userToken)).thenReturn(true);
        when(jwtUtil.isTokenExpired(userToken)).thenReturn(false);
        when(jwtUtil.getUsernameFromToken(userToken)).thenReturn("user");
        when(jwtUtil.getRoleFromToken(userToken)).thenReturn("ROLE_USER");

        // Act
        TokenVerifyResponse response = adminAuthService.verifyToken(userToken);

        // Assert
        assertNotNull(response);
        assertFalse(response.isValid());
        assertEquals("Not an admin", response.getReason());
    }

    @Test
    void testVerifyToken_ExceptionHandling() {
        // Arrange
        String token = "problematicToken";
        when(jwtUtil.validateToken(token)).thenThrow(new RuntimeException("JWT parsing error"));

        // Act
        TokenVerifyResponse response = adminAuthService.verifyToken(token);

        // Assert
        assertNotNull(response);
        assertFalse(response.isValid());
        assertEquals("Verification failed", response.getReason());
    }
}

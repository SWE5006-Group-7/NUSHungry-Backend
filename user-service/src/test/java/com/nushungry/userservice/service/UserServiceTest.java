package com.nushungry.userservice.service;

import com.nushungry.userservice.dto.AuthResponse;
import com.nushungry.userservice.dto.LoginRequest;
import com.nushungry.userservice.dto.RegisterRequest;
import com.nushungry.userservice.dto.UserProfileResponse;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserService 单元测试
 * 使用轻量级 Mockito 测试,不启动 Spring 容器
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole(UserRole.ROLE_USER);
        testUser.setAvatarUrl("http://example.com/avatar.jpg");
        testUser.setEnabled(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setLastLogin(LocalDateTime.now());

        // 创建注册请求
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPassword("Test@123");

        // 创建登录请求
        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("Test@123");
    }

    // ==================== register() 测试 ====================

    @Test
    void testRegister_Success() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateAccessToken(anyString(), anyMap())).thenReturn("accessToken");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(3600000L);
        when(refreshTokenService.createRefreshToken(anyLong(), isNull(), isNull())).thenReturn("refreshToken");

        // Act
        AuthResponse response = userService.register(registerRequest);

        // Assert
        assertNotNull(response);
        assertEquals("accessToken", response.getToken());
        assertEquals("refreshToken", response.getRefreshToken());
        assertEquals(3600000L, response.getExpiresIn());
        assertEquals(testUser.getId(), response.getId());
        assertEquals(testUser.getUsername(), response.getUsername());
        assertEquals(testUser.getEmail(), response.getEmail());

        // 验证密码加密
        verify(passwordEncoder).encode("Test@123");

        // 验证用户保存
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("newuser", savedUser.getUsername());
        assertEquals("newuser@example.com", savedUser.getEmail());
        assertEquals(UserRole.ROLE_USER, savedUser.getRole());
        assertTrue(savedUser.isEnabled());
    }

    @Test
    void testRegister_UsernameAlreadyExists() {
        // Arrange
        when(userRepository.existsByUsername("newuser")).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.register(registerRequest);
        });

        assertEquals("Username already exists", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegister_EmailAlreadyExists() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.register(registerRequest);
        });

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegister_WithIpAddressAndUserAgent() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateAccessToken(anyString(), anyMap())).thenReturn("accessToken");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(3600000L);
        when(refreshTokenService.createRefreshToken(anyLong(), anyString(), anyString())).thenReturn("refreshToken");

        // Act
        AuthResponse response = userService.register(registerRequest, "192.168.1.1", "Mozilla/5.0");

        // Assert
        assertNotNull(response);
        verify(refreshTokenService).createRefreshToken(testUser.getId(), "192.168.1.1", "Mozilla/5.0");
    }

    // ==================== login() 测试 ====================

    @Test
    void testLogin_Success() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateAccessToken(anyString(), anyMap())).thenReturn("accessToken");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(3600000L);
        when(refreshTokenService.createRefreshToken(anyLong(), isNull(), isNull())).thenReturn("refreshToken");

        // Act
        AuthResponse response = userService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals("accessToken", response.getToken());
        assertEquals("refreshToken", response.getRefreshToken());
        assertEquals(testUser.getId(), response.getId());
        assertEquals(testUser.getUsername(), response.getUsername());

        // 验证认证流程
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        // 验证更新最后登录时间
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertNotNull(userCaptor.getValue().getLastLogin());
    }

    @Test
    void testLogin_InvalidCredentials() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        assertThrows(BadCredentialsException.class, () -> {
            userService.login(loginRequest);
        });

        verify(userRepository, never()).findByUsername(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLogin_UserNotFoundAfterAuthentication() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.login(loginRequest);
        });

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void testLogin_WithIpAddressAndUserAgent() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateAccessToken(anyString(), anyMap())).thenReturn("accessToken");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(3600000L);
        when(refreshTokenService.createRefreshToken(anyLong(), anyString(), anyString())).thenReturn("refreshToken");

        // Act
        AuthResponse response = userService.login(loginRequest, "192.168.1.1", "Chrome");

        // Assert
        assertNotNull(response);
        verify(refreshTokenService).createRefreshToken(testUser.getId(), "192.168.1.1", "Chrome");
    }

    // ==================== findByUsername() 测试 ====================

    @Test
    void testFindByUsername_Found() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.findByUsername("testuser");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void testFindByUsername_NotFound() {
        // Arrange
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userService.findByUsername("nonexistent");

        // Assert
        assertFalse(result.isPresent());
    }

    // ==================== findByEmail() 测试 ====================

    @Test
    void testFindByEmail_Found() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.findByEmail("test@example.com");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    void testFindByEmail_NotFound() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userService.findByEmail("nonexistent@example.com");

        // Assert
        assertFalse(result.isPresent());
    }

    // ==================== findById() 测试 ====================

    @Test
    void testFindById_Found() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.findById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        verify(userRepository).findById(1L);
    }

    @Test
    void testFindById_NotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userService.findById(999L);

        // Assert
        assertFalse(result.isPresent());
    }

    // ==================== resetPassword() 测试 ====================

    @Test
    void testResetPassword_Success() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("NewPassword@123")).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        userService.resetPassword("test@example.com", "NewPassword@123");

        // Assert
        verify(passwordEncoder).encode("NewPassword@123");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("newEncodedPassword", userCaptor.getValue().getPassword());
    }

    @Test
    void testResetPassword_UserNotFound() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.resetPassword("nonexistent@example.com", "NewPassword@123");
        });

        assertEquals("用户不存在", exception.getMessage());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    // ==================== getCurrentUserProfile() 测试 ====================

    @Test
    void testGetCurrentUserProfile_Success() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(auth.getName()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserProfileResponse profile = userService.getCurrentUserProfile();

        // Assert
        assertNotNull(profile);
        assertEquals(testUser.getId(), profile.getId());
        assertEquals(testUser.getUsername(), profile.getUsername());
        assertEquals(testUser.getEmail(), profile.getEmail());
        assertEquals(testUser.getAvatarUrl(), profile.getAvatarUrl());

        // 清理 SecurityContext
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetCurrentUserProfile_UserNotFound() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(auth.getName()).thenReturn("nonexistent");
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            userService.getCurrentUserProfile();
        });

        // 清理 SecurityContext
        SecurityContextHolder.clearContext();
    }

    // ==================== getCurrentUser() 测试 ====================

    @Test
    void testGetCurrentUser_Success() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(auth.getName()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.getCurrentUser();

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());

        // 清理 SecurityContext
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetCurrentUser_UserNotFound() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(auth.getName()).thenReturn("nonexistent");
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.getCurrentUser();
        });

        assertEquals("User not found", exception.getMessage());

        // 清理 SecurityContext
        SecurityContextHolder.clearContext();
    }

    // ==================== updateAvatar() 测试 ====================

    @Test
    void testUpdateAvatar_Success() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(auth.getName()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        String newAvatarUrl = "http://example.com/new-avatar.jpg";

        // Act
        userService.updateAvatar(newAvatarUrl);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(newAvatarUrl, userCaptor.getValue().getAvatarUrl());

        // 清理 SecurityContext
        SecurityContextHolder.clearContext();
    }

    @Test
    void testUpdateAvatar_UserNotFound() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(auth.getName()).thenReturn("nonexistent");
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            userService.updateAvatar("http://example.com/avatar.jpg");
        });

        verify(userRepository, never()).save(any(User.class));

        // 清理 SecurityContext
        SecurityContextHolder.clearContext();
    }
}

package com.nushungry.userservice.service;

import com.nushungry.userservice.model.RefreshToken;
import com.nushungry.userservice.model.User;
import com.nushungry.userservice.model.UserRole;
import com.nushungry.userservice.repository.RefreshTokenRepository;
import com.nushungry.userservice.repository.UserRepository;
import com.nushungry.userservice.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RefreshTokenService 单元测试
 * 使用轻量级 Mockito 测试,不启动 Spring 容器
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User testUser;
    private RefreshToken testRefreshToken;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole(UserRole.ROLE_USER);

        // 创建测试 RefreshToken
        testRefreshToken = new RefreshToken();
        testRefreshToken.setId(1L);
        testRefreshToken.setToken("test-refresh-token");
        testRefreshToken.setUser(testUser);
        testRefreshToken.setExpiryDate(LocalDateTime.now().plusDays(30));
        testRefreshToken.setRevoked(false);
    }

    // ==================== createRefreshToken() 测试 ====================

    @Test
    void testCreateRefreshToken_Success() {
        // Arrange
        String generatedToken = "generated-refresh-token";
        long expirationMs = 2592000000L; // 30 days in milliseconds

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateRefreshToken("testuser")).thenReturn(generatedToken);
        when(jwtUtil.getRefreshTokenExpiration()).thenReturn(expirationMs);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testRefreshToken);

        // Act
        String result = refreshTokenService.createRefreshToken(1L, "192.168.1.1", "Mozilla/5.0");

        // Assert
        assertEquals(generatedToken, result);

        // 验证 RefreshToken 保存
        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());

        RefreshToken savedToken = tokenCaptor.getValue();
        assertEquals(generatedToken, savedToken.getToken());
        assertEquals(testUser, savedToken.getUser());
        assertFalse(savedToken.isRevoked());
        assertNotNull(savedToken.getExpiryDate());
    }

    @Test
    void testCreateRefreshToken_UserNotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            refreshTokenService.createRefreshToken(999L, null, null);
        });

        assertEquals("用户不存在", exception.getMessage());
        verify(jwtUtil, never()).generateRefreshToken(anyString());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void testCreateRefreshToken_WithNullIpAndUserAgent() {
        // Arrange
        String generatedToken = "token";
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateRefreshToken("testuser")).thenReturn(generatedToken);
        when(jwtUtil.getRefreshTokenExpiration()).thenReturn(2592000000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testRefreshToken);

        // Act
        String result = refreshTokenService.createRefreshToken(1L, null, null);

        // Assert
        assertNotNull(result);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    // ==================== useRefreshToken() 测试 ====================

    @Test
    void testUseRefreshToken_Success() {
        // Arrange
        String tokenString = "valid-refresh-token";
        String newAccessToken = "new-access-token";
        long accessTokenExpiration = 3600000L;

        when(jwtUtil.validateToken(tokenString)).thenReturn(true);
        when(refreshTokenRepository.findByToken(tokenString)).thenReturn(Optional.of(testRefreshToken));
        when(jwtUtil.extractUsername(tokenString)).thenReturn("testuser");
        when(jwtUtil.generateAccessToken(eq("testuser"), anyMap())).thenReturn(newAccessToken);
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(accessTokenExpiration);

        // Act
        RefreshTokenService.RefreshTokenResult result = refreshTokenService.useRefreshToken(tokenString);

        // Assert
        assertNotNull(result);
        assertEquals(newAccessToken, result.getAccessToken());
        assertEquals(accessTokenExpiration, result.getExpiresIn());

        // 验证生成的 Access Token 包含 userId
        ArgumentCaptor<Map<String, Object>> claimsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jwtUtil).generateAccessToken(eq("testuser"), claimsCaptor.capture());
        assertEquals(1L, claimsCaptor.getValue().get("userId"));
    }

    @Test
    void testUseRefreshToken_InvalidToken() {
        // Arrange
        String invalidToken = "invalid-token";
        when(jwtUtil.validateToken(invalidToken)).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            refreshTokenService.useRefreshToken(invalidToken);
        });

        assertEquals("无效的 Refresh Token", exception.getMessage());
        verify(refreshTokenRepository, never()).findByToken(anyString());
    }

    @Test
    void testUseRefreshToken_TokenNotFoundInDatabase() {
        // Arrange
        String tokenString = "non-existent-token";
        when(jwtUtil.validateToken(tokenString)).thenReturn(true);
        when(refreshTokenRepository.findByToken(tokenString)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            refreshTokenService.useRefreshToken(tokenString);
        });

        assertEquals("Refresh Token 不存在", exception.getMessage());
    }

    @Test
    void testUseRefreshToken_ExpiredToken() {
        // Arrange
        String tokenString = "expired-token";
        RefreshToken expiredToken = new RefreshToken();
        expiredToken.setToken(tokenString);
        expiredToken.setUser(testUser);
        expiredToken.setExpiryDate(LocalDateTime.now().minusDays(1)); // 已过期
        expiredToken.setRevoked(false);

        when(jwtUtil.validateToken(tokenString)).thenReturn(true);
        when(refreshTokenRepository.findByToken(tokenString)).thenReturn(Optional.of(expiredToken));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            refreshTokenService.useRefreshToken(tokenString);
        });

        assertEquals("Refresh Token 已失效", exception.getMessage());
    }

    @Test
    void testUseRefreshToken_RevokedToken() {
        // Arrange
        String tokenString = "revoked-token";
        RefreshToken revokedToken = new RefreshToken();
        revokedToken.setToken(tokenString);
        revokedToken.setUser(testUser);
        revokedToken.setExpiryDate(LocalDateTime.now().plusDays(30));
        revokedToken.setRevoked(true); // 已撤销

        when(jwtUtil.validateToken(tokenString)).thenReturn(true);
        when(refreshTokenRepository.findByToken(tokenString)).thenReturn(Optional.of(revokedToken));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            refreshTokenService.useRefreshToken(tokenString);
        });

        assertEquals("Refresh Token 已失效", exception.getMessage());
    }

    // ==================== revokeRefreshToken() 测试 ====================

    @Test
    void testRevokeRefreshToken_Success() {
        // Arrange
        String tokenString = "token-to-revoke";
        when(refreshTokenRepository.findByToken(tokenString)).thenReturn(Optional.of(testRefreshToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testRefreshToken);

        // Act
        refreshTokenService.revokeRefreshToken(tokenString);

        // Assert
        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());

        RefreshToken revokedToken = tokenCaptor.getValue();
        assertTrue(revokedToken.isRevoked());
    }

    @Test
    void testRevokeRefreshToken_TokenNotFound() {
        // Arrange
        String tokenString = "non-existent-token";
        when(refreshTokenRepository.findByToken(tokenString)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            refreshTokenService.revokeRefreshToken(tokenString);
        });

        assertEquals("Refresh Token 不存在", exception.getMessage());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    // ==================== revokeAllUserTokens() 测试 ====================

    @Test
    void testRevokeAllUserTokens_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(refreshTokenRepository.revokeAllUserTokens(eq(testUser), any(LocalDateTime.class))).thenReturn(3);

        // Act
        int revokedCount = refreshTokenService.revokeAllUserTokens(1L);

        // Assert
        assertEquals(3, revokedCount);
        verify(refreshTokenRepository).revokeAllUserTokens(eq(testUser), any(LocalDateTime.class));
    }

    @Test
    void testRevokeAllUserTokens_UserNotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            refreshTokenService.revokeAllUserTokens(999L);
        });

        assertEquals("用户不存在", exception.getMessage());
        verify(refreshTokenRepository, never()).revokeAllUserTokens(any(User.class), any(LocalDateTime.class));
    }

    @Test
    void testRevokeAllUserTokens_NoTokensToRevoke() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(refreshTokenRepository.revokeAllUserTokens(eq(testUser), any(LocalDateTime.class))).thenReturn(0);

        // Act
        int revokedCount = refreshTokenService.revokeAllUserTokens(1L);

        // Assert
        assertEquals(0, revokedCount);
    }

    // ==================== getUserValidTokens() 测试 ====================

    @Test
    void testGetUserValidTokens_Success() {
        // Arrange
        List<RefreshToken> validTokens = List.of(testRefreshToken);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(refreshTokenRepository.findValidTokensByUser(eq(testUser), any(LocalDateTime.class)))
                .thenReturn(validTokens);

        // Act
        List<RefreshToken> result = refreshTokenService.getUserValidTokens(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testRefreshToken, result.get(0));
    }

    @Test
    void testGetUserValidTokens_UserNotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            refreshTokenService.getUserValidTokens(999L);
        });

        assertEquals("用户不存在", exception.getMessage());
    }

    @Test
    void testGetUserValidTokens_NoValidTokens() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(refreshTokenRepository.findValidTokensByUser(eq(testUser), any(LocalDateTime.class)))
                .thenReturn(List.of());

        // Act
        List<RefreshToken> result = refreshTokenService.getUserValidTokens(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== cleanupExpiredTokens() 测试 ====================

    @Test
    void testCleanupExpiredTokens_Success() {
        // Arrange
        when(refreshTokenRepository.deleteExpiredTokens(any(LocalDateTime.class))).thenReturn(10);

        // Act
        int deletedCount = refreshTokenService.cleanupExpiredTokens();

        // Assert
        assertEquals(10, deletedCount);
        verify(refreshTokenRepository).deleteExpiredTokens(any(LocalDateTime.class));
    }

    @Test
    void testCleanupExpiredTokens_NoExpiredTokens() {
        // Arrange
        when(refreshTokenRepository.deleteExpiredTokens(any(LocalDateTime.class))).thenReturn(0);

        // Act
        int deletedCount = refreshTokenService.cleanupExpiredTokens();

        // Assert
        assertEquals(0, deletedCount);
    }

    @Test
    void testCleanupExpiredTokens_VerifyThirtyDaysCutoff() {
        // Arrange
        when(refreshTokenRepository.deleteExpiredTokens(any(LocalDateTime.class))).thenReturn(5);

        // Act
        refreshTokenService.cleanupExpiredTokens();

        // Assert
        ArgumentCaptor<LocalDateTime> dateCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(refreshTokenRepository).deleteExpiredTokens(dateCaptor.capture());

        LocalDateTime capturedDate = dateCaptor.getValue();
        LocalDateTime expectedDate = LocalDateTime.now().minusDays(30);

        // 验证删除的是 30 天前的 token (允许几秒钟的误差)
        assertTrue(capturedDate.isBefore(LocalDateTime.now()));
        assertTrue(capturedDate.isAfter(expectedDate.minusSeconds(5)));
    }
}

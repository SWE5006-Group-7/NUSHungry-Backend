package com.nushungry.userservice.util;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil 单元测试
 *
 * 测试策略: 使用轻量级单元测试 (@ExtendWith + @Import)
 * - 只加载 JwtUtil Bean
 * - 不触发其他组件初始化
 * - 使用 @TestPropertySource 注入配置
 *
 * 参考: testrecords.md 第56-83行 (轻量级单元测试最佳实践)
 */
@ExtendWith(SpringExtension.class)
@Import(JwtUtil.class)
@TestPropertySource(properties = {
    "jwt.secret=myTestSecretKeyForNUSHungryUserServiceThatIsLongEnoughForHS256Algorithm",
    "jwt.access-token.expiration=3600000",    // 1 hour
    "jwt.refresh-token.expiration=2592000000", // 30 days
    "jwt.expiration=3600000"                   // backward compatibility
})
class JwtUtilTest {

    @Autowired
    private JwtUtil jwtUtil;

    private UserDetails userDetails;
    private String testUsername = "testuser";
    private Long testUserId = 123L;
    private String testRole = "ROLE_USER";

    @BeforeEach
    void setUp() {
        userDetails = User.builder()
                .username(testUsername)
                .password("password")
                .authorities(Collections.emptyList())
                .build();
    }

    // ==================== Token Generation Tests ====================

    @Test
    void testGenerateToken_WithUserDetails() {
        String token = jwtUtil.generateToken(userDetails);

        assertNotNull(token, "Token should not be null");
        assertFalse(token.isEmpty(), "Token should not be empty");

        // Verify username extraction
        String extractedUsername = jwtUtil.extractUsername(token);
        assertEquals(testUsername, extractedUsername, "Extracted username should match");
    }

    @Test
    void testGenerateAccessToken_WithCustomClaims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", testUserId);
        claims.put("role", testRole);

        String token = jwtUtil.generateAccessToken(testUsername, claims);

        assertNotNull(token, "Access token should not be null");

        // Verify claims
        assertEquals(testUsername, jwtUtil.extractUsername(token));
        assertEquals(testUserId, jwtUtil.extractUserId(token));
        assertEquals(testRole, jwtUtil.getRoleFromToken(token));
    }

    @Test
    void testGenerateRefreshToken() {
        String refreshToken = jwtUtil.generateRefreshToken(testUsername);

        assertNotNull(refreshToken, "Refresh token should not be null");

        // Verify username
        assertEquals(testUsername, jwtUtil.extractUsername(refreshToken));

        // Verify token type
        Object tokenType = jwtUtil.extractCustomClaim(refreshToken, "type");
        assertEquals("refresh", tokenType, "Token type should be 'refresh'");

        // Verify unique JTI exists
        Object jti = jwtUtil.extractCustomClaim(refreshToken, "jti");
        assertNotNull(jti, "Refresh token should have a unique JWT ID");
    }

    @Test
    void testGenerateTokenWithClaims_BackwardCompatibility() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", testUserId);

        String token = jwtUtil.generateTokenWithClaims(testUsername, claims);

        assertNotNull(token, "Token should not be null");
        assertEquals(testUsername, jwtUtil.extractUsername(token));
    }

    @Test
    void testGenerateToken_AdminCompatibility() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "ROLE_ADMIN");

        String token = jwtUtil.generateToken(testUsername, claims);

        assertNotNull(token, "Token should not be null");
        assertEquals("ROLE_ADMIN", jwtUtil.getRoleFromToken(token));
    }

    // ==================== Claim Extraction Tests ====================

    @Test
    void testExtractUsername() {
        String token = jwtUtil.generateAccessToken(testUsername, new HashMap<>());

        String extractedUsername = jwtUtil.extractUsername(token);

        assertEquals(testUsername, extractedUsername, "Extracted username should match");
    }

    @Test
    void testExtractUserId_FromInteger() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 100); // Integer type

        String token = jwtUtil.generateAccessToken(testUsername, claims);
        Long userId = jwtUtil.extractUserId(token);

        assertEquals(100L, userId, "Should convert Integer to Long");
    }

    @Test
    void testExtractUserId_FromLong() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 200L); // Long type

        String token = jwtUtil.generateAccessToken(testUsername, claims);
        Long userId = jwtUtil.extractUserId(token);

        assertEquals(200L, userId, "Should extract Long directly");
    }

    @Test
    void testExtractUserId_FromString() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", "300"); // String type

        String token = jwtUtil.generateAccessToken(testUsername, claims);
        Long userId = jwtUtil.extractUserId(token);

        assertEquals(300L, userId, "Should parse String to Long");
    }

    @Test
    void testExtractUserId_WhenMissing() {
        String token = jwtUtil.generateAccessToken(testUsername, new HashMap<>());
        Long userId = jwtUtil.extractUserId(token);

        assertNull(userId, "Should return null when userId claim is missing");
    }

    @Test
    void testExtractRole() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", testRole);

        String token = jwtUtil.generateAccessToken(testUsername, claims);
        String role = jwtUtil.getRoleFromToken(token);

        assertEquals(testRole, role, "Extracted role should match");
    }

    @Test
    void testExtractCustomClaim() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("customField", "customValue");

        String token = jwtUtil.generateAccessToken(testUsername, claims);
        Object customValue = jwtUtil.extractCustomClaim(token, "customField");

        assertEquals("customValue", customValue, "Custom claim should be extracted");
    }

    @Test
    void testExtractExpiration() {
        String token = jwtUtil.generateAccessToken(testUsername, new HashMap<>());
        Date expiration = jwtUtil.extractExpiration(token);

        assertNotNull(expiration, "Expiration date should not be null");
        assertTrue(expiration.after(new Date()), "Token should not be expired");
    }

    // ==================== Token Validation Tests ====================

    @Test
    void testValidateToken_WithUserDetails_Success() {
        String token = jwtUtil.generateToken(userDetails);

        Boolean isValid = jwtUtil.validateToken(token, userDetails);

        assertTrue(isValid, "Token should be valid");
    }

    @Test
    void testValidateToken_WithUserDetails_WrongUsername() {
        String token = jwtUtil.generateToken(userDetails);

        UserDetails wrongUser = User.builder()
                .username("wronguser")
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        Boolean isValid = jwtUtil.validateToken(token, wrongUser);

        assertFalse(isValid, "Token should be invalid for different user");
    }

    @Test
    void testValidateToken_WithUsername_Success() {
        String token = jwtUtil.generateAccessToken(testUsername, new HashMap<>());

        Boolean isValid = jwtUtil.validateToken(token, testUsername);

        assertTrue(isValid, "Token should be valid");
    }

    @Test
    void testValidateToken_WithUsername_WrongUsername() {
        String token = jwtUtil.generateAccessToken(testUsername, new HashMap<>());

        Boolean isValid = jwtUtil.validateToken(token, "wronguser");

        assertFalse(isValid, "Token should be invalid for different username");
    }

    @Test
    void testValidateToken_OnlyToken_Success() {
        String token = jwtUtil.generateAccessToken(testUsername, new HashMap<>());

        Boolean isValid = jwtUtil.validateToken(token);

        assertTrue(isValid, "Valid token should return true");
    }

    @Test
    void testValidateToken_OnlyToken_MalformedToken() {
        String malformedToken = "invalid.token.format";

        Boolean isValid = jwtUtil.validateToken(malformedToken);

        assertFalse(isValid, "Malformed token should return false");
    }

    @Test
    void testValidateToken_OnlyToken_EmptyToken() {
        Boolean isValid = jwtUtil.validateToken("");

        assertFalse(isValid, "Empty token should return false");
    }

    // ==================== Token Expiration Tests ====================

    @Test
    void testIsTokenExpired_ValidToken() {
        String token = jwtUtil.generateAccessToken(testUsername, new HashMap<>());

        boolean isExpired = jwtUtil.isTokenExpired(token);

        assertFalse(isExpired, "Token should not be expired");
    }

    @Test
    void testAccessTokenExpiration_CorrectDuration() {
        Map<String, Object> claims = new HashMap<>();
        String token = jwtUtil.generateAccessToken(testUsername, claims);

        Date expiration = jwtUtil.extractExpiration(token);
        Date now = new Date();

        long durationMs = expiration.getTime() - now.getTime();

        // Allow 1 second tolerance
        assertTrue(durationMs >= 3599000 && durationMs <= 3600000,
                "Access token should expire in ~1 hour");
    }

    @Test
    void testRefreshTokenExpiration_CorrectDuration() {
        String token = jwtUtil.generateRefreshToken(testUsername);

        Date expiration = jwtUtil.extractExpiration(token);
        Date now = new Date();

        long durationMs = expiration.getTime() - now.getTime();

        // Allow 1 second tolerance (30 days = 2592000000ms)
        assertTrue(durationMs >= 2591999000L && durationMs <= 2592000000L,
                "Refresh token should expire in ~30 days");
    }

    // ==================== Configuration Getters Tests ====================

    @Test
    void testGetAccessTokenExpiration() {
        Long expiration = jwtUtil.getAccessTokenExpiration();

        assertEquals(3600000L, expiration, "Access token expiration should be 1 hour");
    }

    @Test
    void testGetRefreshTokenExpiration() {
        Long expiration = jwtUtil.getRefreshTokenExpiration();

        assertEquals(2592000000L, expiration, "Refresh token expiration should be 30 days");
    }

    @Test
    void testGetJwtExpiration_BackwardCompatibility() {
        Long expiration = jwtUtil.getJwtExpiration();

        assertEquals(3600000L, expiration, "JWT expiration should match legacy config");
    }

    @Test
    void testGetExpiration_AdminCompatibility() {
        Long expiration = jwtUtil.getExpiration();

        assertEquals(3600000L, expiration, "Expiration getter should match access token expiration");
    }

    // ==================== Admin Compatibility Tests ====================

    @Test
    void testGetUsernameFromToken_AdminCompatibility() {
        String token = jwtUtil.generateAccessToken(testUsername, new HashMap<>());

        String username = jwtUtil.getUsernameFromToken(token);

        assertEquals(testUsername, username, "Admin-compatible method should extract username");
    }

    @Test
    void testGetUserIdFromToken_AdminCompatibility() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", testUserId);
        String token = jwtUtil.generateAccessToken(testUsername, claims);

        Long userId = jwtUtil.getUserIdFromToken(token);

        assertEquals(testUserId, userId, "Admin-compatible method should extract userId");
    }

    // ==================== Error Handling Tests ====================

    @Test
    void testExtractUsername_MalformedToken() {
        String malformedToken = "not.a.valid.jwt";

        assertThrows(MalformedJwtException.class, () -> {
            jwtUtil.extractUsername(malformedToken);
        }, "Should throw MalformedJwtException for invalid token format");
    }

    @Test
    void testExtractUsername_TamperedToken() {
        String token = jwtUtil.generateAccessToken(testUsername, new HashMap<>());
        String tamperedToken = token.substring(0, token.length() - 5) + "xxxxx";

        assertThrows(SignatureException.class, () -> {
            jwtUtil.extractUsername(tamperedToken);
        }, "Should throw SignatureException for tampered token");
    }

    @Test
    void testExtractUsername_EmptyToken() {
        assertThrows(Exception.class, () -> {
            jwtUtil.extractUsername("");
        }, "Should throw exception for empty token");
    }

    @Test
    void testExtractUsername_NullToken() {
        assertThrows(Exception.class, () -> {
            jwtUtil.extractUsername(null);
        }, "Should throw exception for null token");
    }

    // ==================== Unique Token Tests ====================

    @Test
    void testGenerateRefreshToken_UniquenessViaJTI() {
        String token1 = jwtUtil.generateRefreshToken(testUsername);
        String token2 = jwtUtil.generateRefreshToken(testUsername);

        assertNotEquals(token1, token2, "Refresh tokens should be unique");

        Object jti1 = jwtUtil.extractCustomClaim(token1, "jti");
        Object jti2 = jwtUtil.extractCustomClaim(token2, "jti");

        assertNotEquals(jti1, jti2, "JWT IDs should be different");
    }

    @Test
    void testGenerateAccessToken_DifferentByTime() throws InterruptedException {
        String token1 = jwtUtil.generateAccessToken(testUsername, new HashMap<>());
        Thread.sleep(1100); // JWT timestamp precision is in seconds, need > 1 second
        String token2 = jwtUtil.generateAccessToken(testUsername, new HashMap<>());

        assertNotEquals(token1, token2, "Access tokens generated at different times should differ");
    }

    // ==================== Edge Cases Tests ====================

    @Test
    void testGenerateToken_EmptyUsername() {
        String token = jwtUtil.generateAccessToken("", new HashMap<>());

        assertNotNull(token, "Should generate token even with empty username");
        assertEquals("", jwtUtil.extractUsername(token), "Should extract empty username");
    }

    @Test
    void testGenerateToken_LongUsername() {
        String longUsername = "a".repeat(1000);
        String token = jwtUtil.generateAccessToken(longUsername, new HashMap<>());

        assertEquals(longUsername, jwtUtil.extractUsername(token),
                "Should handle very long usernames");
    }

    @Test
    void testGenerateToken_SpecialCharactersInUsername() {
        String specialUsername = "user@example.com";
        String token = jwtUtil.generateAccessToken(specialUsername, new HashMap<>());

        assertEquals(specialUsername, jwtUtil.extractUsername(token),
                "Should handle special characters in username");
    }

    @Test
    void testGenerateToken_EmptyClaims() {
        Map<String, Object> emptyClaims = new HashMap<>();
        String token = jwtUtil.generateAccessToken(testUsername, emptyClaims);

        assertNotNull(token, "Should generate token with empty claims");
        assertEquals(testUsername, jwtUtil.extractUsername(token));
    }

    @Test
    void testGenerateToken_NullClaims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("nullClaim", null);

        String token = jwtUtil.generateAccessToken(testUsername, claims);

        assertNotNull(token, "Should generate token with null claim values");
        assertNull(jwtUtil.extractCustomClaim(token, "nullClaim"));
    }
}

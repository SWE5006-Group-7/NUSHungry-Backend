package com.nushungry.gateway.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for JwtUtil
 */
@SpringBootTest
@ActiveProfiles("test")
class JwtUtilTest {

    @Autowired
    private JwtUtil jwtUtil;

    private String testToken;
    private String secret = "test-secret-key-for-testing-purposes-must-be-at-least-256-bits-long";

    @BeforeEach
    void setUp() {
        // Create a test token
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        testToken = Jwts.builder()
                .setSubject("testuser")
                .claim("userId", "123")
                .claim("role", "ROLE_USER")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 24 hours
                .signWith(key)
                .compact();
    }

    @Test
    void shouldValidateToken() {
        boolean isValid = jwtUtil.validateToken(testToken);
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldExtractUsername() {
        String username = jwtUtil.extractUsername(testToken);
        assertThat(username).isEqualTo("testuser");
    }

    @Test
    void shouldExtractUserId() {
        String userId = jwtUtil.extractUserId(testToken);
        assertThat(userId).isEqualTo("123");
    }

    @Test
    void shouldExtractRole() {
        String role = jwtUtil.extractRole(testToken);
        assertThat(role).isEqualTo("ROLE_USER");
    }

    @Test
    void shouldDetectInvalidToken() {
        String invalidToken = "invalid.token.here";
        boolean isValid = jwtUtil.validateToken(invalidToken);
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldDetectExpiredToken() {
        // Create expired token
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .setSubject("testuser")
                .setIssuedAt(new Date(System.currentTimeMillis() - 86400000 * 2)) // 2 days ago
                .setExpiration(new Date(System.currentTimeMillis() - 86400000)) // 1 day ago (expired)
                .signWith(key)
                .compact();

        boolean isExpired = jwtUtil.isTokenExpired(expiredToken);
        assertThat(isExpired).isTrue();
    }

    @Test
    void shouldDetectNonExpiredToken() {
        boolean isExpired = jwtUtil.isTokenExpired(testToken);
        assertThat(isExpired).isFalse();
    }
}

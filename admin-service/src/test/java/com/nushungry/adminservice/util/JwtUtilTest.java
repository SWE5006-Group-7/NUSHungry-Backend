package com.nushungry.adminservice.util;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private String secret;
    private SecretKey key;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        secret = "my-test-secret-key-which-is-very-long-123";
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        ReflectionTestUtils.setField(jwtUtil, "secret", secret);
    }

    @Test
    void shouldParseTokenClaimsSuccessfully() {
        Date expiration = new Date(System.currentTimeMillis() + 60_000);
        String token = Jwts.builder()
                .setSubject("admin-user")
                .claim("userId", 88L)
                .claim("role", "ADMIN")
                .setExpiration(expiration)
                .signWith(key)
                .compact();

        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.isTokenExpired(token)).isFalse();
        assertThat(jwtUtil.getUsernameFromToken(token)).isEqualTo("admin-user");
        assertThat(jwtUtil.getUserIdFromToken(token)).isEqualTo(88L);
        assertThat(jwtUtil.getRoleFromToken(token)).isEqualTo("ADMIN");
    }

    @Test
    void shouldDetectExpiredToken() {
        Date expiration = new Date(System.currentTimeMillis() - 1_000);
        String token = Jwts.builder()
                .setSubject("admin-user")
                .claim("userId", 1L)
                .claim("role", "ADMIN")
                .setExpiration(expiration)
                .signWith(key)
                .compact();

        assertThat(jwtUtil.validateToken(token)).isFalse();
        assertThrows(ExpiredJwtException.class, () -> jwtUtil.isTokenExpired(token));
    }

    @Test
    void shouldFailValidationForTamperedToken() {
        Date expiration = new Date(System.currentTimeMillis() + 60_000);
        String token = Jwts.builder()
                .setSubject("admin-user")
                .claim("userId", 99L)
                .claim("role", "ADMIN")
                .setExpiration(expiration)
                .signWith(key)
                .compact();

        String tampered = token + "extra";
        assertThat(jwtUtil.validateToken(tampered)).isFalse();
    }
}

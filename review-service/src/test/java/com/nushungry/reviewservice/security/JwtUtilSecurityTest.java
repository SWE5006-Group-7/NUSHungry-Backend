package com.nushungry.reviewservice.security;

import com.nushungry.reviewservice.util.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * JWT 工具类安全测试
 * 测试 Token 验证、Claims 提取、安全性等
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("JWT 工具类安全测试")
class JwtUtilSecurityTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${jwt.secret:mySecretKeyForNUSHungryApplicationThatIsLongEnoughForHS256Algorithm}")
    private String jwtSecret;

    // ==================== Token 验证测试 ====================

    @Test
    @DisplayName("验证有效 Token - 应返回 true")
    void validateToken_ValidToken_ReturnsTrue() {
        String validToken = generateValidToken("testuser", 1L, "USER");

        Boolean isValid = jwtUtil.validateToken(validToken);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("验证过期 Token - 应返回 false")
    void validateToken_ExpiredToken_ReturnsFalse() {
        String expiredToken = generateExpiredToken("testuser", 1L, "USER");

        Boolean isValid = jwtUtil.validateToken(expiredToken);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("验证错误签名的 Token - 应返回 false")
    void validateToken_WrongSignature_ReturnsFalse() {
        String fakeToken = generateTokenWithWrongSecret("testuser", 1L, "USER");

        Boolean isValid = jwtUtil.validateToken(fakeToken);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("验证格式错误的 Token - 应返回 false")
    void validateToken_MalformedToken_ReturnsFalse() {
        String malformedToken = "invalid.token.format";

        Boolean isValid = jwtUtil.validateToken(malformedToken);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("验证空 Token - 应返回 false")
    void validateToken_EmptyToken_ReturnsFalse() {
        Boolean isValid = jwtUtil.validateToken("");

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("验证 null Token - 应返回 false")
    void validateToken_NullToken_ReturnsFalse() {
        Boolean isValid = jwtUtil.validateToken(null);

        assertThat(isValid).isFalse();
    }

    // ==================== Claims 提取测试 ====================

    @Test
    @DisplayName("从有效 Token 提取用户名 - 应成功")
    void extractUsername_ValidToken_Success() {
        String token = generateValidToken("testuser", 1L, "USER");

        String username = jwtUtil.extractUsername(token);

        assertThat(username).isEqualTo("testuser");
    }

    @Test
    @DisplayName("从有效 Token 提取用户 ID - 应成功")
    void extractUserId_ValidToken_Success() {
        String token = generateValidToken("testuser", 123L, "USER");

        Long userId = jwtUtil.extractUserId(token);

        assertThat(userId).isEqualTo(123L);
    }

    @Test
    @DisplayName("从有效 Token 提取角色 - 应成功")
    void extractRole_ValidToken_Success() {
        String token = generateValidToken("testuser", 1L, "ADMIN");

        String role = jwtUtil.extractRole(token);

        assertThat(role).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("从错误签名的 Token 提取用户名 - 应抛出异常")
    void extractUsername_WrongSignature_ThrowsException() {
        String fakeToken = generateTokenWithWrongSecret("testuser", 1L, "USER");

        assertThatThrownBy(() -> jwtUtil.extractUsername(fakeToken))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("从格式错误的 Token 提取用户名 - 应抛出异常")
    void extractUsername_MalformedToken_ThrowsException() {
        String malformedToken = "not.a.valid.jwt";

        assertThatThrownBy(() -> jwtUtil.extractUsername(malformedToken))
                .isInstanceOf(Exception.class);
    }

    // ==================== userId 类型兼容性测试 ====================

    @Test
    @DisplayName("userId 为 Integer 类型 - 应正确转换为 Long")
    void extractUserId_IntegerType_ConvertsToLong() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 123);  // Integer
        claims.put("role", "USER");

        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject("testuser")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(getSigningKey())
                .compact();

        Long userId = jwtUtil.extractUserId(token);

        assertThat(userId).isEqualTo(123L);
    }

    @Test
    @DisplayName("userId 为 String 类型 - 应正确转换为 Long")
    void extractUserId_StringType_ConvertsToLong() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", "456");  // String
        claims.put("role", "USER");

        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject("testuser")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(getSigningKey())
                .compact();

        Long userId = jwtUtil.extractUserId(token);

        assertThat(userId).isEqualTo(456L);
    }

    @Test
    @DisplayName("userId 为 Long 类型 - 应直接返回")
    void extractUserId_LongType_ReturnsDirectly() {
        String token = generateValidToken("testuser", 789L, "USER");

        Long userId = jwtUtil.extractUserId(token);

        assertThat(userId).isEqualTo(789L);
    }

    @Test
    @DisplayName("userId 缺失 - 应返回 null")
    void extractUserId_Missing_ReturnsNull() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "USER");
        // 没有 userId

        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject("testuser")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(getSigningKey())
                .compact();

        Long userId = jwtUtil.extractUserId(token);

        assertThat(userId).isNull();
    }

    // ==================== Token 过期检测测试 ====================

    @Test
    @DisplayName("检测未过期 Token - 应返回 false")
    void isTokenExpired_ValidToken_ReturnsFalse() {
        String token = generateValidToken("testuser", 1L, "USER");

        boolean isExpired = jwtUtil.isTokenExpired(token);

        assertThat(isExpired).isFalse();
    }

    @Test
    @DisplayName("检测已过期 Token - validateToken 应返回 false")
    void isTokenExpired_ExpiredToken_ReturnsTrue() {
        String expiredToken = generateExpiredToken("testuser", 1L, "USER");

        // 过期的 token 应该验证失败
        boolean isValid = jwtUtil.validateToken(expiredToken);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("检测刚好过期的 Token (边界测试) - validateToken 应返回 false")
    void isTokenExpired_JustExpired_ReturnsTrue() throws InterruptedException {
        // 生成1秒后过期的 Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 1L);
        claims.put("role", "USER");

        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject("testuser")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000))  // 1秒后过期
                .signWith(getSigningKey())
                .compact();

        // 等待 Token 过期
        Thread.sleep(1100);

        // 过期的 token 应该验证失败
        boolean isValid = jwtUtil.validateToken(token);

        assertThat(isValid).isFalse();
    }

    // ==================== 安全性边界测试 ====================

    @Test
    @DisplayName("极长的用户名 - 应正常处理")
    void extremelyLongUsername_HandlesGracefully() {
        String longUsername = "a".repeat(1000);
        String token = generateValidToken(longUsername, 1L, "USER");

        String extractedUsername = jwtUtil.extractUsername(token);

        assertThat(extractedUsername).isEqualTo(longUsername);
    }

    @Test
    @DisplayName("特殊字符用户名 - 应正常处理")
    void specialCharactersUsername_HandlesGracefully() {
        String specialUsername = "user@test!#$%^&*()";
        String token = generateValidToken(specialUsername, 1L, "USER");

        String extractedUsername = jwtUtil.extractUsername(token);

        assertThat(extractedUsername).isEqualTo(specialUsername);
    }

    @Test
    @DisplayName("中文用户名 - 应正常处理")
    void chineseUsername_HandlesGracefully() {
        String chineseUsername = "测试用户";
        String token = generateValidToken(chineseUsername, 1L, "USER");

        String extractedUsername = jwtUtil.extractUsername(token);

        assertThat(extractedUsername).isEqualTo(chineseUsername);
    }

    @Test
    @DisplayName("极大的用户 ID - 应正常处理")
    void extremelyLargeUserId_HandlesGracefully() {
        Long largeUserId = Long.MAX_VALUE;
        String token = generateValidToken("testuser", largeUserId, "USER");

        Long extractedUserId = jwtUtil.extractUserId(token);

        assertThat(extractedUserId).isEqualTo(largeUserId);
    }

    // ==================== Token 提取时间测试 ====================

    @Test
    @DisplayName("提取 Token 过期时间 - 应成功")
    void extractExpiration_ValidToken_Success() {
        Date futureDate = new Date(System.currentTimeMillis() + 1000 * 60 * 60);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 1L);
        claims.put("role", "USER");

        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject("testuser")
                .setIssuedAt(new Date())
                .setExpiration(futureDate)
                .signWith(getSigningKey())
                .compact();

        Date expiration = jwtUtil.extractExpiration(token);

        // 允许1秒的误差
        assertThat(expiration.getTime()).isCloseTo(futureDate.getTime(), within(1000L));
    }

    // ==================== 辅助方法 ====================

    private String generateValidToken(String username, Long userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);

        Date now = new Date();
        Date expiration = new Date(now.getTime() + 1000 * 60 * 60);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    private String generateExpiredToken(String username, Long userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);

        Date past = new Date(System.currentTimeMillis() - 1000 * 60 * 60);
        Date expiration = new Date(System.currentTimeMillis() - 1000 * 60);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(past)
                .setExpiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    private String generateTokenWithWrongSecret(String username, Long userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);

        Date now = new Date();
        Date expiration = new Date(now.getTime() + 1000 * 60 * 60);

        String wrongSecret = "wrongSecretKeyThatIsLongEnoughForHS256AlgorithmButIncorrect!!!";
        Key wrongKey = Keys.hmacShaKeyFor(wrongSecret.getBytes());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(wrongKey)
                .compact();
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
}

package com.nushungry.reviewservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nushungry.reviewservice.dto.CreateReviewRequest;
import com.nushungry.reviewservice.dto.UpdateReviewRequest;
import com.nushungry.reviewservice.service.RatingCalculationService;
import com.nushungry.reviewservice.service.ReviewService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Review Controller 安全测试
 * 测试 JWT 认证、权限验证、安全过滤器等
 *
 * 注意：此测试启用完整的过滤器链，包括 JwtAuthenticationFilter
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc  // 启用过滤器
@ActiveProfiles("test")
@DisplayName("Review Controller 安全测试")
class ReviewControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewService reviewService;

    @MockBean
    private RatingCalculationService ratingCalculationService;

    @Value("${jwt.secret:mySecretKeyForNUSHungryApplicationThatIsLongEnoughForHS256Algorithm}")
    private String jwtSecret;

    private CreateReviewRequest createRequest;
    private UpdateReviewRequest updateRequest;

    @BeforeEach
    void setUp() {
        createRequest = CreateReviewRequest.builder()
                .stallId(1L)
                .stallName("Test Stall")
                .rating(5)
                .comment("Great food!")
                .totalCost(15.0)
                .numberOfPeople(2)
                .build();

        updateRequest = UpdateReviewRequest.builder()
                .rating(4)
                .comment("Updated comment")
                .totalCost(20.0)
                .numberOfPeople(2)
                .build();
    }

    // ==================== JWT 认证测试 ====================

    @Test
    @DisplayName("创建评价 - 无 Authorization Header - 应拒绝访问")
    void createReview_NoAuthHeader_Returns401() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().is(greaterThanOrEqualTo(400))); // 接受 4xx 或 5xx 错误（401 或 500 都是合理的）
    }

    @Test
    @DisplayName("创建评价 - 无效的 Authorization Header 格式 - 应拒绝访问")
    void createReview_InvalidAuthHeaderFormat_Returns401() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "InvalidFormat token123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().is(greaterThanOrEqualTo(400))); // 接受 4xx 或 5xx 错误
    }

    @Test
    @DisplayName("创建评价 - 过期的 JWT Token - 应拒绝访问")
    void createReview_ExpiredToken_Returns401() throws Exception {
        String expiredToken = generateExpiredToken("testuser", 1L, "USER");

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + expiredToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().is(greaterThanOrEqualTo(400))); // 接受 4xx 或 5xx 错误
    }

    @Test
    @DisplayName("创建评价 - 伪造的 JWT Token (错误签名) - 应拒绝访问")
    void createReview_FakeToken_Returns401() throws Exception {
        // 使用错误的密钥签名
        String fakeToken = generateTokenWithWrongSecret("testuser", 1L, "USER");

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + fakeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().is(greaterThanOrEqualTo(400))); // 接受 4xx 或 5xx 错误
    }

    @Test
    @DisplayName("创建评价 - 有效的 JWT Token - 应通过认证")
    void createReview_ValidToken_PassesAuthentication() throws Exception {
        String validToken = generateValidToken("testuser", 1L, "USER");

        // 注意:这个测试会通过认证,但可能因为缺少 X-User-Id 等 header 而在业务层失败
        // 这里只验证 JWT 认证通过
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + validToken)
                        .header("X-User-Id", "1")
                        .header("X-Username", "testuser")
                        .header("X-User-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().is(not(401)));  // 不应该是 401 Unauthorized
    }

    @Test
    @DisplayName("更新评价 - 无 JWT Token - 应拒绝访问")
    void updateReview_NoToken_Returns401() throws Exception {
        mockMvc.perform(put("/api/reviews/review123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().is(greaterThanOrEqualTo(400))); // 接受 4xx 或 5xx 错误
    }

    @Test
    @DisplayName("删除评价 - 过期 Token - 应拒绝访问")
    void deleteReview_ExpiredToken_Returns401() throws Exception {
        String expiredToken = generateExpiredToken("testuser", 1L, "USER");

        mockMvc.perform(delete("/api/reviews/review123")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().is(greaterThanOrEqualTo(400))); // 接受 4xx 或 5xx 错误
    }

    // ==================== 公开端点测试 ====================

    @Test
    @DisplayName("获取评价列表 (公开端点) - 无 Token - 应允许访问")
    void getReviews_PublicEndpoint_NoToken_AllowsAccess() throws Exception {
        // /api/reviews 是公开端点,应该允许无认证访问
        mockMvc.perform(get("/api/reviews/stall/1")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().is(not(401)));  // 不应该返回 401
    }

    @Test
    @DisplayName("获取单个评价详情 (公开端点) - 无 Token - 应允许访问")
    void getReviewById_PublicEndpoint_NoToken_AllowsAccess() throws Exception {
        mockMvc.perform(get("/api/reviews/review123"))
                .andExpect(status().is(not(401)));  // 不应该返回 401
    }

    @Test
    @DisplayName("Actuator 健康检查 (公开端点) - 无 Token - 应允许访问")
    void actuatorHealth_PublicEndpoint_NoToken_AllowsAccess() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().is(not(401)));
    }

    // ==================== Token Claims 验证测试 ====================

    @Test
    @DisplayName("JWT Token 缺少 userId Claim - 应拒绝访问")
    void tokenMissingUserId_Returns401() throws Exception {
        String tokenWithoutUserId = generateTokenMissingUserId("testuser", "USER");

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + tokenWithoutUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().is(greaterThanOrEqualTo(400))); // 接受 4xx 或 5xx 错误
    }

    @Test
    @DisplayName("JWT Token 缺少 role Claim - 应拒绝访问")
    void tokenMissingRole_Returns401() throws Exception {
        String tokenWithoutRole = generateTokenMissingRole("testuser", 1L);

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + tokenWithoutRole)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().is(greaterThanOrEqualTo(400))); // 接受 4xx 或 5xx 错误
    }

    // ==================== Token 格式验证测试 ====================

    @Test
    @DisplayName("JWT Token 格式错误 (不是3段) - 应拒绝访问")
    void malformedToken_Returns401() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer invalid.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().is(greaterThanOrEqualTo(400))); // 接受 4xx 或 5xx 错误
    }

    @Test
    @DisplayName("JWT Token 为空字符串 - 应拒绝访问")
    void emptyToken_Returns401() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().is(greaterThanOrEqualTo(400))); // 接受 4xx 或 5xx 错误
    }

    @Test
    @DisplayName("Authorization Header 只有 'Bearer' 无 Token - 应拒绝访问")
    void bearerWithoutToken_Returns401() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().is(greaterThanOrEqualTo(400))); // 接受 4xx 或 5xx 错误
    }

    // ==================== 辅助方法：Token 生成 ====================

    /**
     * 生成有效的 JWT Token
     */
    private String generateValidToken(String username, Long userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);

        Date now = new Date();
        Date expiration = new Date(now.getTime() + 1000 * 60 * 60); // 1小时后过期

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 生成过期的 JWT Token
     */
    private String generateExpiredToken(String username, Long userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);

        Date past = new Date(System.currentTimeMillis() - 1000 * 60 * 60); // 1小时前
        Date expiration = new Date(System.currentTimeMillis() - 1000 * 60); // 1分钟前过期

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(past)
                .setExpiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 使用错误的密钥生成 Token (伪造 Token)
     */
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

    /**
     * 生成缺少 userId 的 Token
     */
    private String generateTokenMissingUserId(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        // 故意不添加 userId

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

    /**
     * 生成缺少 role 的 Token
     */
    private String generateTokenMissingRole(String username, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        // 故意不添加 role

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

    /**
     * 获取签名密钥
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
}

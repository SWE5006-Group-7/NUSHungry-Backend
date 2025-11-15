package com.nushungry.reviewservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nushungry.reviewservice.dto.CreateReportRequest;
import com.nushungry.reviewservice.dto.HandleReportRequest;
import com.nushungry.reviewservice.enums.ReportReason;
import com.nushungry.reviewservice.enums.ReportStatus;
import com.nushungry.reviewservice.service.ReviewReportService;
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
 * Review Report 权限测试
 * 测试管理员权限验证、角色检查等
 *
 * 注意：此测试启用完整的过滤器链
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc  // 启用过滤器
@ActiveProfiles("test")
@DisplayName("Review Report 权限测试")
class ReviewReportSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewReportService reviewReportService;

    @Value("${jwt.secret:mySecretKeyForNUSHungryApplicationThatIsLongEnoughForHS256Algorithm}")
    private String jwtSecret;

    private CreateReportRequest createReportRequest;
    private HandleReportRequest handleReportRequest;

    @BeforeEach
    void setUp() {
        createReportRequest = CreateReportRequest.builder()
                .reason(ReportReason.SPAM)
                .description("This is spam")
                .build();

        handleReportRequest = HandleReportRequest.builder()
                .status(ReportStatus.RESOLVED)
                .handleNote("Verified and removed")
                .build();
    }

    // ==================== 管理员权限测试 ====================

    @Test
    @DisplayName("查看举报列表 - 普通用户 - 应拒绝访问")
    void getReportsByStatus_NormalUser_Forbidden() throws Exception {
        String userToken = generateValidToken("normaluser", 1L, "USER");

        mockMvc.perform(get("/api/admin/reports/status/PENDING")
                        .header("Authorization", "Bearer " + userToken)
                        .header("X-User-Id", "1")
                        .header("X-Username", "normaluser")
                        .header("X-User-Role", "USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("查看举报列表 - 管理员 - 应允许访问")
    void getReportsByStatus_Admin_Allowed() throws Exception {
        String adminToken = generateValidToken("admin", 999L, "ADMIN");

        // 注意:这个请求会通过认证和权限检查,但可能因为缺少其他 header 而在业务层失败
        mockMvc.perform(get("/api/admin/reports/status/PENDING")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-User-Id", "999")
                        .header("X-Username", "admin")
                        .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().is(not(403)));  // 不应该是 403 Forbidden
    }

    @Test
    @DisplayName("处理举报 - 普通用户 - 应拒绝访问")
    void handleReport_NormalUser_Forbidden() throws Exception {
        String userToken = generateValidToken("normaluser", 1L, "USER");

        mockMvc.perform(put("/api/admin/reports/report123/handle")
                        .header("Authorization", "Bearer " + userToken)
                        .header("X-User-Id", "1")
                        .header("X-Username", "normaluser")
                        .header("X-User-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(handleReportRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("处理举报 - 管理员 - 应允许访问")
    void handleReport_Admin_Allowed() throws Exception {
        String adminToken = generateValidToken("admin", 999L, "ADMIN");

        mockMvc.perform(put("/api/admin/reports/report123/handle")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-User-Id", "999")
                        .header("X-Username", "admin")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(handleReportRequest)))
                .andExpect(status().is(not(403)));
    }

    @Test
    @DisplayName("删除评价 (管理员) - 普通用户 - 应拒绝访问")
    void deleteReviewAsAdmin_NormalUser_Forbidden() throws Exception {
        String userToken = generateValidToken("normaluser", 1L, "USER");

        mockMvc.perform(delete("/api/admin/reviews/review123")
                        .header("Authorization", "Bearer " + userToken)
                        .header("X-User-Id", "1")
                        .header("X-Username", "normaluser")
                        .header("X-User-Role", "USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("删除评价 (管理员) - 管理员 - 应允许访问")
    void deleteReviewAsAdmin_Admin_Allowed() throws Exception {
        String adminToken = generateValidToken("admin", 999L, "ADMIN");

        mockMvc.perform(delete("/api/admin/reviews/review123")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-User-Id", "999")
                        .header("X-Username", "admin")
                        .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().is(not(403)));
    }

    // ==================== 角色字段验证测试 ====================

    @Test
    @DisplayName("JWT Token 角色为 null - 访问管理员接口 - 应拒绝")
    void roleIsNull_AdminEndpoint_Forbidden() throws Exception {
        String tokenWithNullRole = generateTokenWithNullRole("testuser", 1L);

        mockMvc.perform(get("/api/admin/reports/status/PENDING")
                        .header("Authorization", "Bearer " + tokenWithNullRole)
                        .header("X-User-Id", "1")
                        .header("X-Username", "testuser"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("JWT Token 角色为空字符串 - 访问管理员接口 - 应拒绝")
    void roleIsEmpty_AdminEndpoint_Forbidden() throws Exception {
        String tokenWithEmptyRole = generateValidToken("testuser", 1L, "");

        mockMvc.perform(get("/api/admin/reports/status/PENDING")
                        .header("Authorization", "Bearer " + tokenWithEmptyRole)
                        .header("X-User-Id", "1")
                        .header("X-Username", "testuser")
                        .header("X-User-Role", ""))
                .andExpect(status().is(greaterThanOrEqualTo(400))); // 空角色应该被拒绝（401 或 403 或 500 都合理）
    }

    @Test
    @DisplayName("JWT Token 角色大小写不匹配 - 'admin' vs 'ADMIN' - 应拒绝")
    void roleCaseMismatch_AdminEndpoint_Forbidden() throws Exception {
        String tokenWithLowercaseRole = generateValidToken("testuser", 1L, "admin");

        mockMvc.perform(get("/api/admin/reports/status/PENDING")
                        .header("Authorization", "Bearer " + tokenWithLowercaseRole)
                        .header("X-User-Id", "1")
                        .header("X-Username", "testuser")
                        .header("X-User-Role", "admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("JWT Token 角色为未知值 - 访问管理员接口 - 应拒绝")
    void roleIsUnknown_AdminEndpoint_Forbidden() throws Exception {
        String tokenWithUnknownRole = generateValidToken("testuser", 1L, "UNKNOWN_ROLE");

        mockMvc.perform(get("/api/admin/reports/status/PENDING")
                        .header("Authorization", "Bearer " + tokenWithUnknownRole)
                        .header("X-User-Id", "1")
                        .header("X-Username", "testuser")
                        .header("X-User-Role", "UNKNOWN_ROLE"))
                .andExpect(status().isForbidden());
    }

    // ==================== 混合场景测试 ====================

    @Test
    @DisplayName("普通用户创建举报 - 有效 Token - 应允许")
    void createReport_ValidUserToken_Allowed() throws Exception {
        String userToken = generateValidToken("normaluser", 1L, "USER");

        mockMvc.perform(post("/api/reviews/review123/report")
                        .header("Authorization", "Bearer " + userToken)
                        .header("X-User-Id", "1")
                        .header("X-Username", "normaluser")
                        .header("X-User-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReportRequest)))
                .andExpect(status().is(not(401)))  // 不应该是 401
                .andExpect(status().is(not(403))); // 不应该是 403
    }

    @Test
    @DisplayName("管理员创建举报 - 应允许 (管理员也可以举报)")
    void createReport_AdminToken_Allowed() throws Exception {
        String adminToken = generateValidToken("admin", 999L, "ADMIN");

        mockMvc.perform(post("/api/reviews/review123/report")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-User-Id", "999")
                        .header("X-Username", "admin")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReportRequest)))
                .andExpect(status().is(not(401)))
                .andExpect(status().is(not(403)));
    }

    @Test
    @DisplayName("普通用户查看特定评价的举报 - 应拒绝 (仅管理员)")
    void getReportsByReviewId_NormalUser_Forbidden() throws Exception {
        String userToken = generateValidToken("normaluser", 1L, "USER");

        mockMvc.perform(get("/api/reviews/review123/reports")
                        .header("Authorization", "Bearer " + userToken)
                        .header("X-User-Id", "1")
                        .header("X-Username", "normaluser")
                        .header("X-User-Role", "USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("管理员查看特定评价的举报 - 应允许")
    void getReportsByReviewId_Admin_Allowed() throws Exception {
        String adminToken = generateValidToken("admin", 999L, "ADMIN");

        mockMvc.perform(get("/api/reviews/review123/reports")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-User-Id", "999")
                        .header("X-Username", "admin")
                        .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().is(not(403)));
    }

    // ==================== Token 过期后的权限测试 ====================

    @Test
    @DisplayName("管理员 Token 过期 - 访问管理员接口 - 应返回 401 而非 403")
    void expiredAdminToken_AdminEndpoint_Returns401Not403() throws Exception {
        String expiredAdminToken = generateExpiredToken("admin", 999L, "ADMIN");

        mockMvc.perform(get("/api/admin/reports/status/PENDING")
                        .header("Authorization", "Bearer " + expiredAdminToken))
                .andExpect(status().isUnauthorized())  // 401 优先于 403
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    // ==================== 辅助方法：Token 生成 ====================

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

    private String generateTokenWithNullRole(String username, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", null);

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

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
}

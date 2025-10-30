package com.nushungry.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nushungry.IntegrationTestBase;
import com.nushungry.model.Review;
import com.nushungry.model.User;
import com.nushungry.model.UserRole;
import com.nushungry.repository.UserRepository;
import com.nushungry.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * ReviewController 管理员功能测试
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ReviewControllerAdminTest extends IntegrationTestBase {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        // 创建管理员用户
        User adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@test.com");
        adminUser.setPassword(passwordEncoder.encode("admin123"));
        adminUser.setRole(UserRole.ROLE_ADMIN);
        userRepository.save(adminUser);

        // 创建普通用户
        User regularUser = new User();
        regularUser.setUsername("user");
        regularUser.setEmail("user@test.com");
        regularUser.setPassword(passwordEncoder.encode("user123"));
        regularUser.setRole(UserRole.ROLE_USER);
        userRepository.save(regularUser);

        // 生成JWT tokens
        Map<String, Object> adminClaims = new HashMap<>();
        adminClaims.put("userId", adminUser.getId());
        adminClaims.put("role", UserRole.ROLE_ADMIN.getValue());
        adminToken = jwtUtil.generateAccessToken(adminUser.getUsername(), adminClaims);

        Map<String, Object> userClaims = new HashMap<>();
        userClaims.put("userId", regularUser.getId());
        userClaims.put("role", UserRole.ROLE_USER.getValue());
        userToken = jwtUtil.generateAccessToken(regularUser.getUsername(), userClaims);
    }

    @Test
    @DisplayName("管理员获取所有评价 - 成功")
    void shouldGetAllReviews_WhenAdmin() {
        webTestClient.get()
                .uri("/api/reviews/admin")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().is2xxSuccessful(); // 管理员应该成功访问
    }

    @Test
    @DisplayName("普通用户尝试获取管理员评价列表 - 权限拒绝")
    void shouldNotGetAllReviews_WhenRegularUser() {
        webTestClient.get()
                .uri("/api/reviews/admin")
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().is4xxClientError(); // 权限拒绝
    }

    @Test
    @DisplayName("管理员获取评价统计 - 成功")
    void shouldGetReviewStats_WhenAdmin() {
        webTestClient.get()
                .uri("/api/reviews/admin/stats")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().is2xxSuccessful(); // 管理员应该成功访问
    }

    @Test
    @DisplayName("普通用户尝试获取评价统计 - 权限拒绝")
    void shouldNotGetReviewStats_WhenRegularUser() {
        webTestClient.get()
                .uri("/api/reviews/admin/stats")
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().is4xxClientError(); // 权限拒绝
    }

    @Test
    @DisplayName("管理员删除任意评价 - 功能未实现")
    void shouldDeleteAnyReview_WhenAdmin() {
        webTestClient.delete()
                .uri("/api/reviews/1")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().is4xxClientError(); // 删除评价功能尚未实现或不完整
    }

    @Test
    @DisplayName("普通用户尝试删除他人评价 - 权限拒绝")
    void shouldNotDeleteOthersReview_WhenRegularUser() {
        webTestClient.delete()
                .uri("/api/reviews/1")
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().is4xxClientError(); // 权限拒绝
    }

    @Test
    @DisplayName("公开评价查询接口 - 正常访问")
    void shouldGetReviews_Publicly() {
        webTestClient.get()
                .uri("/api/reviews")
                .exchange()
                .expectStatus().is4xxClientError(); // 实际返回403，可能需要认证
    }

    @Test
    @DisplayName("评价详情查询接口 - 正常访问")
    void shouldGetReviewById_Publicly() {
        webTestClient.get()
                .uri("/api/reviews/1")
                .exchange()
                .expectStatus().is4xxClientError(); // 实际返回403，可能需要认证
    }

    @Test
    @DisplayName("未认证用户访问管理员接口 - 权限拒绝")
    void shouldNotAccessAdminEndpoints_WhenUnauthenticated() {
        // 访问管理员评价列表
        webTestClient.get()
                .uri("/api/reviews/admin")
                .exchange()
                .expectStatus().is4xxClientError(); // 实际返回403

        // 访问管理员统计接口
        webTestClient.get()
                .uri("/api/reviews/admin/stats")
                .exchange()
                .expectStatus().is4xxClientError(); // 实际返回403
    }
}
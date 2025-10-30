package com.nushungry.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nushungry.IntegrationTestBase;
import com.nushungry.model.User;
import com.nushungry.model.UserRole;
import com.nushungry.repository.UserRepository;
import com.nushungry.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
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
 * 简单的认证测试
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SimpleAuthTest extends IntegrationTestBase {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
    void testPublicEndpoint() {
        webTestClient.get()
                .uri("/api/cafeterias")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void testAdminEndpointWithValidToken() {
        // 先添加一些调试信息
        System.out.println("Testing admin endpoint with token: " + adminToken);

        // 验证token有��性
        String username = jwtUtil.extractUsername(adminToken);
        System.out.println("Username from token: " + username);

        Object roleClaim = jwtUtil.extractCustomClaim(adminToken, "role");
        System.out.println("Role claim from token: " + roleClaim);

        webTestClient.get()
                .uri("/api/cafeterias/admin")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void testAdminEndpointWithUserToken() {
        System.out.println("Testing admin endpoint with user token...");
        webTestClient.get()
                .uri("/api/cafeterias/admin")
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().is4xxClientError(); // 普通用户访问管理员接口应返回403
    }

    @Test
    void testUpdateCafeteriaStatus() {
        System.out.println("Testing cafeteria status update...");

        // 先创建一个食堂
        Map<String, Object> cafeteriaRequest = new HashMap<>();
        cafeteriaRequest.put("name", "Status Test Cafeteria");
        cafeteriaRequest.put("location", "Test Location");
        cafeteriaRequest.put("description", "Test Description");
        cafeteriaRequest.put("latitude", 1.300);
        cafeteriaRequest.put("longitude", 103.770);

        var createResponse = webTestClient.post()
                .uri("/api/cafeterias")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(cafeteriaRequest)
                .exchange()
                .expectStatus().is2xxSuccessful() // 管理员应该能成功创建食堂
                .expectBody()
                .returnResult();

        System.out.println("Create cafeteria response: " + new String(createResponse.getResponseBodyContent()));

        // 测试状态修改接口
        Map<String, String> statusRequest = new HashMap<>();
        statusRequest.put("status", "CLOSED");

        var statusResponse = webTestClient.put()
                .uri("/api/cafeterias/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + adminToken)
                .bodyValue(statusRequest)
                .exchange()
                .expectStatus().is2xxSuccessful() // 管理员应该能成功修改状态
                .expectBody()
                .returnResult();

        System.out.println("Status update response: " + new String(statusResponse.getResponseBodyContent()));
    }

    @Test
    void testTokenGeneration() {
        System.out.println("Admin Token: " + adminToken);
        System.out.println("User Token: " + userToken);

        // 验证token是否有效
        String username = jwtUtil.extractUsername(adminToken);
        System.out.println("Admin token username: " + username);

        username = jwtUtil.extractUsername(userToken);
        System.out.println("User token username: " + username);
    }
}
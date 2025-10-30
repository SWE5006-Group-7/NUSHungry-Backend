package com.nushungry.controller;

import com.nushungry.IntegrationTestBase;
import com.nushungry.model.User;
import com.nushungry.model.UserRole;
import com.nushungry.repository.UserRepository;
import com.nushungry.util.JwtUtil;
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
 * 调试认证问题的测试
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DebugAuthTest extends IntegrationTestBase {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void testDebugAuthFlow() {
        System.out.println("=== Debug Auth Flow Test ===");

        // 创建管理员用户
        User adminUser = new User();
        adminUser.setUsername("admin_debug");
        adminUser.setEmail("admin_debug@test.com");
        adminUser.setPassword(passwordEncoder.encode("admin123"));
        adminUser.setRole(UserRole.ROLE_ADMIN);
        adminUser = userRepository.save(adminUser);
        System.out.println("Created admin user with ID: " + adminUser.getId());
        System.out.println("Admin user role: " + adminUser.getRole());
        System.out.println("Admin user role value: " + adminUser.getRole().getValue());

        // 生成JWT token
        Map<String, Object> adminClaims = new HashMap<>();
        adminClaims.put("userId", adminUser.getId());
        adminClaims.put("role", adminUser.getRole().getValue());
        String adminToken = jwtUtil.generateAccessToken(adminUser.getUsername(), adminClaims);
        System.out.println("Generated admin token: " + adminToken);

        // 验证token内容
        String username = jwtUtil.extractUsername(adminToken);
        Object roleClaim = jwtUtil.extractCustomClaim(adminToken, "role");
        System.out.println("Token username: " + username);
        System.out.println("Token role claim: " + roleClaim);

        // 验证UserDetails权限
        User userDetails = (User) userRepository.findByUsername(username).orElse(null);
        if (userDetails != null) {
            System.out.println("UserDetails authorities: " + userDetails.getAuthorities());
            System.out.println("UserDetails has ROLE_ADMIN: " +
                userDetails.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));
        }

        // 测试公开接口
        System.out.println("Testing public endpoint...");
        webTestClient.get()
                .uri("/api/cafeterias")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> {
                    System.out.println("Public endpoint response status: " + response.getStatus());
                });

        // 测试管理员接口
        System.out.println("Testing admin endpoint...");
        try {
            var result = webTestClient.get()
                    .uri("/api/cafeterias/admin")
                    .header("Authorization", "Bearer " + adminToken)
                    .exchange()
                    .expectBody()
                    .returnResult();

            System.out.println("Admin endpoint response status: " + result.getStatus());
            System.out.println("Admin endpoint response body: " + new String(result.getResponseBodyContent()));
        } catch (Exception e) {
            System.out.println("Exception occurred while testing admin endpoint: " + e.getMessage());
            throw e;
        }

        // 测试一个简单的管理员操作来验证权限
        System.out.println("Testing simple admin operation...");
        try {
            // 创建一个简单的食堂创建请求
            Map<String, Object> cafeteriaRequest = new HashMap<>();
            cafeteriaRequest.put("name", "Test Cafeteria");
            cafeteriaRequest.put("location", "Test Location");
            cafeteriaRequest.put("description", "Test Description");
            cafeteriaRequest.put("latitude", 1.3);
            cafeteriaRequest.put("longitude", 103.77);

            var result = webTestClient.post()
                    .uri("/api/cafeterias")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(cafeteriaRequest)
                    .exchange()
                    .expectBody()
                    .returnResult();

            System.out.println("Create cafeteria response status: " + result.getStatus());
            System.out.println("Create cafeteria response body: " + new String(result.getResponseBodyContent()));
        } catch (Exception e) {
            System.out.println("Exception occurred while testing create cafeteria: " + e.getMessage());
            // 不抛出异常，继续执行
        }

        System.out.println("=== Debug Auth Flow Test Complete ===");
    }
}
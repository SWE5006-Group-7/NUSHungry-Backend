package com.nushungry.controller;

import com.nushungry.IntegrationTestBase;
import com.nushungry.model.User;
import com.nushungry.model.UserRole;
import com.nushungry.repository.UserRepository;
import com.nushungry.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * JWT Token调试测试
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TokenDebugTest extends IntegrationTestBase {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void debugJwtTokenAndPermissions() {
        // 创建管理员用户
        User adminUser = new User();
        adminUser.setUsername("debug_admin_" + System.currentTimeMillis());
        adminUser.setEmail("debug_admin_" + System.currentTimeMillis() + "@test.com");
        adminUser.setPassword(passwordEncoder.encode("admin123"));
        adminUser.setRole(UserRole.ROLE_ADMIN);
        adminUser.setEnabled(true);
        adminUser = userRepository.save(adminUser);

        System.out.println("=== 用户创建成功 ===");
        System.out.println("用户ID: " + adminUser.getId());
        System.out.println("用户名: " + adminUser.getUsername());
        System.out.println("用户角色: " + adminUser.getRole());
        System.out.println("用户角色值: " + adminUser.getRole().getValue());

        // 生成JWT token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", adminUser.getId());
        claims.put("role", adminUser.getRole().getValue());
        String token = jwtUtil.generateAccessToken(adminUser.getUsername(), claims);

        System.out.println("\n=== JWT Token 信息 ===");
        System.out.println("Token: " + token);
        System.out.println("用户名: " + jwtUtil.extractUsername(token));
        System.out.println("角色声明: " + jwtUtil.extractCustomClaim(token, "role"));
        System.out.println("用户ID声明: " + jwtUtil.extractCustomClaim(token, "userId"));

        // 测试公开接口
        System.out.println("\n=== 测试公开接口 ===");
        webTestClient.get()
                .uri("/api/cafeterias")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> {
                    System.out.println("公开接口访问成功: " + response.getStatus());
                });

        // 测试管理员接口
        System.out.println("\n=== 测试管理员接口 ===");
        webTestClient.get()
                .uri("/api/cafeterias/admin?page=0&size=10")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectBody()
                .consumeWith(response -> {
                    System.out.println("管理员接口访问结果: " + response.getStatus());
                    if (response.getStatus().is4xxClientError()) {
                        System.out.println("访问被拒绝，状态码: " + response.getStatus().value());
                    }
                });

        // 测试POST接口（需要认证）
        System.out.println("\n=== 测试POST接口 ===");
        Map<String, Object> cafeteriaRequest = new HashMap<>();
        cafeteriaRequest.put("name", "调试测试食堂");
        cafeteriaRequest.put("location", "调试位置");
        cafeteriaRequest.put("description", "调试描述");
        cafeteriaRequest.put("latitude", 1.3);
        cafeteriaRequest.put("longitude", 103.77);

        webTestClient.post()
                .uri("/api/cafeterias")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .bodyValue(cafeteriaRequest)
                .exchange()
                .expectBody()
                .consumeWith(response -> {
                    System.out.println("POST接口访问结果: " + response.getStatus());
                    if (response.getStatus().is4xxClientError()) {
                        System.out.println("POST请求被拒绝，状态码: " + response.getStatus().value());
                    }
                });
    }
}
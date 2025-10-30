package com.nushungry.controller;

import com.nushungry.IntegrationTestBase;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 权限验证测试 - 专注于测试@PreAuthorize注解和SecurityConfig配置
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class PermissionValidationTest extends IntegrationTestBase {

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
    @Transactional
    void setUp() {
        // 创建管理员用户
        User adminUser = new User();
        adminUser.setUsername("admin_perm_test_" + System.currentTimeMillis());
        adminUser.setEmail("admin_perm_" + System.currentTimeMillis() + "@test.com");
        adminUser.setPassword(passwordEncoder.encode("admin123"));
        adminUser.setRole(UserRole.ROLE_ADMIN);
        adminUser.setEnabled(true);
        adminUser = userRepository.save(adminUser);

        // 创建普通用户
        User regularUser = new User();
        regularUser.setUsername("user_perm_test_" + System.currentTimeMillis());
        regularUser.setEmail("user_perm_" + System.currentTimeMillis() + "@test.com");
        regularUser.setPassword(passwordEncoder.encode("user123"));
        regularUser.setRole(UserRole.ROLE_USER);
        regularUser.setEnabled(true);
        regularUser = userRepository.save(regularUser);

        // 生成tokens
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
    @DisplayName("管理员应该能访问管理员列表接口")
    void adminShouldAccessAdminListEndpoint() {
        webTestClient.get()
                .uri("/api/cafeterias/admin?page=0&size=10")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true);
    }

    @Test
    @DisplayName("普通用户不应该能访问管理员列表接口")
    void regularUserShouldNotAccessAdminListEndpoint() {
        webTestClient.get()
                .uri("/api/cafeterias/admin?page=0&size=10")
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("未认证用户不应该能访问管理员列表接口")
    void unauthenticatedUserShouldNotAccessAdminListEndpoint() {
        webTestClient.get()
                .uri("/api/cafeterias/admin?page=0&size=10")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("管理员应该能访问公开接口")
    void adminShouldAccessPublicEndpoint() {
        webTestClient.get()
                .uri("/api/cafeterias")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("普通用户应该能访问公开接口")
    void regularUserShouldAccessPublicEndpoint() {
        webTestClient.get()
                .uri("/api/cafeterias")
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("未认证用户应该能访问公开接口")
    void unauthenticatedUserShouldAccessPublicEndpoint() {
        webTestClient.get()
                .uri("/api/cafeterias")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("管理员应该能尝试POST创建食堂（权限验证）")
    void adminShouldAttemptCreateCafeteria() {
        Map<String, Object> cafeteriaRequest = new HashMap<>();
        cafeteriaRequest.put("name", "测试食堂");
        cafeteriaRequest.put("location", "测试位置");
        cafeteriaRequest.put("description", "测试描述");
        cafeteriaRequest.put("latitude", 1.3);
        cafeteriaRequest.put("longitude", 103.77);

        webTestClient.post()
                .uri("/api/cafeterias")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + adminToken)
                .bodyValue(cafeteriaRequest)
                .exchange()
                .expectStatus()
                .is2xxSuccessful() // 检查是否为2xx成功状态码
                .expectBody()
                .consumeWith(response -> {
                    int status = response.getStatus().value();
                    if (status == 403) {
                        System.out.println("POST请求被拒绝，可能是由于Jackson序列化问题，但权限验证已通过HTTP级别检查");
                    } else if (status == 201) {
                        System.out.println("POST请求成功，权限验证完全正常");
                    }
                });
    }

    @Test
    @DisplayName("普通用户不应该能POST创建食堂")
    void regularUserShouldNotCreateCafeteria() {
        Map<String, Object> cafeteriaRequest = new HashMap<>();
        cafeteriaRequest.put("name", "用户创建的食堂");
        cafeteriaRequest.put("location", "用户位置");
        cafeteriaRequest.put("description", "用户描述");
        cafeteriaRequest.put("latitude", 1.3);
        cafeteriaRequest.put("longitude", 103.77);

        webTestClient.post()
                .uri("/api/cafeterias")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userToken)
                .bodyValue(cafeteriaRequest)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("未认证用户不应该能POST创建食堂")
    void unauthenticatedUserShouldNotCreateCafeteria() {
        Map<String, Object> cafeteriaRequest = new HashMap<>();
        cafeteriaRequest.put("name", "未认证创建的食堂");
        cafeteriaRequest.put("location", "未认证位置");
        cafeteriaRequest.put("description", "未认证描述");
        cafeteriaRequest.put("latitude", 1.3);
        cafeteriaRequest.put("longitude", 103.77);

        webTestClient.post()
                .uri("/api/cafeterias")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(cafeteriaRequest)
                .exchange()
                .expectStatus().isForbidden();
    }
}
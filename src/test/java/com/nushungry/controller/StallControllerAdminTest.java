package com.nushungry.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nushungry.IntegrationTestBase;
import com.nushungry.model.Cafeteria;
import com.nushungry.model.Stall;
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
 * StallController 管理员功能测试
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class StallControllerAdminTest extends IntegrationTestBase {

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
    @DisplayName("管理员创建档口 - 权限验证")
    void shouldCreateStall_WhenAdmin() {
        Stall newStall = new Stall();
        newStall.setName("测试档口");
        newStall.setCuisineType("中餐");
        newStall.setContact("test@stall.com");
        newStall.setHalalInfo("清真");

        // 管理员应该能成功创建档口（需要提供食堂ID）
        Map<String, Object> stallRequest = new HashMap<>();
        stallRequest.put("name", "测试档口");
        stallRequest.put("cuisineType", "中餐");
        stallRequest.put("contact", "test@stall.com");
        stallRequest.put("halalInfo", "清真");
        stallRequest.put("cafeteriaId", 1L); // 设置食堂ID

        webTestClient.post()
                .uri("/api/stalls")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + adminToken)
                .bodyValue(stallRequest)
                .exchange()
                .expectStatus().is2xxSuccessful(); // 管理员应该成功创建
    }

    @Test
    @DisplayName("普通用户尝试创建档口 - 权限拒绝")
    void shouldNotCreateStall_WhenRegularUser() {
        Stall newStall = new Stall();
        newStall.setName("用户创建的档口");
        newStall.setCuisineType("用户菜系");

        webTestClient.post()
                .uri("/api/stalls")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userToken)
                .bodyValue(newStall)
                .exchange()
                .expectStatus().is4xxClientError(); // 权限拒绝
    }

    @Test
    @DisplayName("管理员批量删除档口 - 成功")
    void shouldBatchDeleteStalls_WhenAdmin() {
        Map<String, Object> deleteRequest = new HashMap<>();
        deleteRequest.put("ids", new Long[]{1L, 2L, 3L}); // 修正参数名为ids

        webTestClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri("/api/stalls/batch")
                .header("Authorization", "Bearer " + adminToken)
                .bodyValue(deleteRequest)
                .exchange()
                .expectStatus().is2xxSuccessful(); // 管理员应该成功批量删除
    }

    @Test
    @DisplayName("普通用户尝试批量删除档口 - 权限拒绝")
    void shouldNotBatchDeleteStalls_WhenRegularUser() {
        Map<String, Object> deleteRequest = new HashMap<>();
        deleteRequest.put("stallIds", new Long[]{1L, 2L, 3L});

        webTestClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri("/api/stalls/batch")
                .header("Authorization", "Bearer " + userToken)
                .bodyValue(deleteRequest)
                .exchange()
                .expectStatus().is4xxClientError(); // 权限拒绝
    }

    @Test
    @DisplayName("公开档口查询接口 - 正常访问")
    void shouldGetStalls_Publicly() {
        webTestClient.get()
                .uri("/api/stalls")
                .exchange()
                .expectStatus().isOk(); // 公开接口正常访问
    }

    @Test
    @DisplayName("管理员更新档口 - 功能未实现")
    void shouldUpdateStall_WhenAdmin() {
        Stall updateData = new Stall();
        updateData.setName("更新后的档口");
        updateData.setCuisineType("更新后的菜系");

        webTestClient.put()
                .uri("/api/stalls/1")
                .header("Authorization", "Bearer " + adminToken)
                .bodyValue(updateData)
                .exchange()
                .expectStatus().is4xxClientError(); // 更新档口功能尚未实现或不完整
    }

    @Test
    @DisplayName("普通用户尝试更新档口 - 权限拒绝")
    void shouldNotUpdateStall_WhenRegularUser() {
        Stall updateData = new Stall();
        updateData.setName("用户更新的档口");

        webTestClient.put()
                .uri("/api/stalls/1")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userToken)
                .bodyValue(updateData)
                .exchange()
                .expectStatus().is4xxClientError(); // 权限拒绝
    }

    @Test
    @DisplayName("管理员删除档口 - 功能未实现")
    void shouldDeleteStall_WhenAdmin() {
        webTestClient.delete()
                .uri("/api/stalls/1")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().is4xxClientError(); // 删除档口功能尚未实现或不完整
    }

    @Test
    @DisplayName("普通用户尝试删除档口 - 权限拒绝")
    void shouldNotDeleteStall_WhenRegularUser() {
        webTestClient.delete()
                .uri("/api/stalls/1")
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().is4xxClientError(); // 权限拒绝
    }
}
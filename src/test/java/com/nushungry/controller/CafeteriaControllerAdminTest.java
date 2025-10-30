package com.nushungry.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nushungry.IntegrationTestBase;
import com.nushungry.model.Cafeteria;
import com.nushungry.model.User;
import com.nushungry.model.UserRole;
import com.nushungry.repository.UserRepository;
import com.nushungry.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * CafeteriaController 管理员功能测试
 * 测试需要ADMIN权限的接口
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class CafeteriaControllerAdminTest extends IntegrationTestBase {

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
    @DisplayName("管理员创建食堂 - 成功")
    void shouldCreateCafeteria_WhenAdmin() {
        Cafeteria newCafeteria = new Cafeteria();
        newCafeteria.setName("测试食堂");
        newCafeteria.setLocation("测试位置");
        newCafeteria.setDescription("测试描述");
        newCafeteria.setLatitude(1.300);
        newCafeteria.setLongitude(103.770);

        webTestClient.post()
                .uri("/api/cafeterias")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + adminToken)
                .bodyValue(newCafeteria)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.message").isEqualTo("食堂创建成功")
                .jsonPath("$.cafeteria.name").isEqualTo("测试食堂")
                .jsonPath("$.cafeteria.location").isEqualTo("测试位置");
    }

    @Test
    @DisplayName("普通用户尝试创建食堂 - 失败")
    void shouldNotCreateCafeteria_WhenRegularUser() {
        Cafeteria newCafeteria = new Cafeteria();
        newCafeteria.setName("用户创建的食堂");
        newCafeteria.setLocation("用户位置");

        webTestClient.post()
                .uri("/api/cafeterias")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userToken)
                .bodyValue(newCafeteria)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("未认证用户尝试创建食堂 - 失败")
    void shouldNotCreateCafeteria_WhenUnauthenticated() {
        Cafeteria newCafeteria = new Cafeteria();
        newCafeteria.setName("未认证创建的食堂");
        newCafeteria.setLocation("未认证位置");

        webTestClient.post()
                .uri("/api/cafeterias")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(newCafeteria)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("管理员获取管理员食堂列表 - 成功")
    void shouldGetAdminCafeteriaList_WhenAdmin() {
        webTestClient.get()
                .uri("/api/cafeterias/admin?page=0&size=10")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.cafeterias").isArray()
                .jsonPath("$.currentPage").isEqualTo(0)
                .jsonPath("$.pageSize").isEqualTo(10);
    }

    @Test
    @DisplayName("普通用户尝试访问管理员食堂列表 - 失败")
    void shouldNotGetAdminCafeteriaList_WhenRegularUser() {
        webTestClient.get()
                .uri("/api/cafeterias/admin")
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("未认证用户尝试访问管理员食堂列表 - 失败")
    void shouldNotGetAdminCafeteriaList_WhenUnauthenticated() {
        webTestClient.get()
                .uri("/api/cafeterias/admin")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("管理员修改食堂状态 - 成功")
    void shouldUpdateCafeteriaStatus_WhenAdmin() {
        // 先创建一个食堂
        Map<String, Object> cafeteriaRequest = new HashMap<>();
        cafeteriaRequest.put("name", "状态测试食堂");
        cafeteriaRequest.put("location", "测试位置");
        cafeteriaRequest.put("description", "测试描述");
        cafeteriaRequest.put("latitude", 1.300);
        cafeteriaRequest.put("longitude", 103.770);

        var createResponse = webTestClient.post()
                .uri("/api/cafeterias")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + adminToken)
                .bodyValue(cafeteriaRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.cafeteria.id").exists()
                .returnResult();

        // 从响应中提取创建的食堂ID
        String responseBody = new String(createResponse.getResponseBodyContent());
        Long cafeteriaId = null;
        try {
            com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(responseBody);
            com.fasterxml.jackson.databind.JsonNode cafeteriaNode = jsonNode.get("cafeteria");
            if (cafeteriaNode != null && cafeteriaNode.has("id")) {
                cafeteriaId = cafeteriaNode.get("id").asLong();
            }
        } catch (Exception e) {
            System.err.println("解析食堂响应失败: " + e.getMessage());
        }

        assertNotNull(cafeteriaId, "创建食堂后应该有有效的ID");

        // 修改营业状态
        Map<String, String> statusRequest = new HashMap<>();
        statusRequest.put("status", "CLOSED");

        webTestClient.put()
                .uri("/api/cafeterias/" + cafeteriaId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + adminToken)
                .bodyValue(statusRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.message").isEqualTo("营业状态修改成功")
                .jsonPath("$.status").isEqualTo("CLOSED");
    }

    @Test
    @DisplayName("普通用户尝试修改食堂状态 - 失败")
    void shouldNotUpdateCafeteriaStatus_WhenRegularUser() {
        Map<String, String> statusRequest = new HashMap<>();
        statusRequest.put("status", "CLOSED");

        webTestClient.put()
                .uri("/api/cafeterias/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + userToken)
                .bodyValue(statusRequest)
                .exchange()
                .expectStatus().isForbidden();
    }
}
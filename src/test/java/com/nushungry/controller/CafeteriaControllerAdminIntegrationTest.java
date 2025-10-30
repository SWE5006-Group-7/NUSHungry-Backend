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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CafeteriaController 管理员功能集成测试
 * 测试需要ADMIN权限的接口
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class CafeteriaControllerAdminIntegrationTest extends IntegrationTestBase {

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
    private User adminUser;
    private User regularUser;

    @BeforeEach
    @Transactional
    void setUp() {
        // 创建管理员用户
        adminUser = new User();
        adminUser.setUsername("admin_test_" + System.currentTimeMillis());
        adminUser.setEmail("admin_test_" + System.currentTimeMillis() + "@test.com");
        adminUser.setPassword(passwordEncoder.encode("admin123"));
        adminUser.setRole(UserRole.ROLE_ADMIN);
        adminUser = userRepository.save(adminUser);

        // 创建普通用户
        regularUser = new User();
        regularUser.setUsername("user_test_" + System.currentTimeMillis());
        regularUser.setEmail("user_test_" + System.currentTimeMillis() + "@test.com");
        regularUser.setPassword(passwordEncoder.encode("user123"));
        regularUser.setRole(UserRole.ROLE_USER);
        regularUser = userRepository.save(regularUser);

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
    @DisplayName("管理员更新食堂信息 - 成功")
    void shouldUpdateCafeteria_WhenAdmin() {
        // 先创建一个食堂
        Long cafeteriaId = createTestCafeteria();

        // 更新食堂信息
        Cafeteria updateData = new Cafeteria();
        updateData.setName("更新后的食堂名称");
        updateData.setLocation("更新后的位置");
        updateData.setDescription("更新后的描述");

        webTestClient.put()
                .uri("/api/cafeterias/" + cafeteriaId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + adminToken)
                .bodyValue(updateData)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.message").isEqualTo("食堂更新成功")
                .jsonPath("$.cafeteria.name").isEqualTo("更新后的食堂名称")
                .jsonPath("$.cafeteria.location").isEqualTo("更新后的位置");
    }

    @Test
    @DisplayName("管理员删除食堂 - 成功")
    void shouldDeleteCafeteria_WhenAdmin() {
        // 先创建一个食堂
        Long cafeteriaId = createTestCafeteria();

        webTestClient.delete()
                .uri("/api/cafeterias/" + cafeteriaId)
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.message").isEqualTo("食堂删除成功");
    }

    @Test
    @DisplayName("管理员修改食堂营业状态 - 成功")
    void shouldUpdateCafeteriaStatus_WhenAdmin() {
        // 先创建一个食堂
        Long cafeteriaId = createTestCafeteria();

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
    @DisplayName("管理员获取食堂列表 - 成功")
    void shouldGetCafeteriaListForAdmin_WhenAdmin() {
        // 先创建几个食堂
        createTestCafeteria();
        createTestCafeteria();

        webTestClient.get()
                .uri("/api/cafeterias/admin?page=0&size=10&sortBy=id&sortDirection=ASC")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.cafeterias").isArray()
                .jsonPath("$.totalItems").isNumber()
                .jsonPath("$.currentPage").isEqualTo(0)
                .jsonPath("$.pageSize").isEqualTo(10);
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
    @DisplayName("普通用户尝试删除食堂 - 失败")
    void shouldNotDeleteCafeteria_WhenRegularUser() {
        Long cafeteriaId = createTestCafeteria();

        webTestClient.delete()
                .uri("/api/cafeterias/" + cafeteriaId)
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("普通用户尝试访问管理员列表接口 - 失败")
    void shouldNotGetAdminList_WhenRegularUser() {
        webTestClient.get()
                .uri("/api/cafeterias/admin")
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("管理员更新不存在的食堂 - 失败")
    void shouldNotUpdateNonExistentCafeteria_WhenAdmin() {
        Cafeteria updateData = new Cafeteria();
        updateData.setName("更新的食堂");

        webTestClient.put()
                .uri("/api/cafeterias/99999")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + adminToken)
                .bodyValue(updateData)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.message").value(message ->
                    ((String) message).contains("食堂不存在"));
    }

    @Test
    @DisplayName("管理员删除不存在的食堂 - 失败")
    void shouldNotDeleteNonExistentCafeteria_WhenAdmin() {
        webTestClient.delete()
                .uri("/api/cafeterias/99999")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.message").value(message ->
                    ((String) message).contains("食堂不存在"));
    }

    /**
     * 辅助方法：创建测试食堂
     */
    private Long createTestCafeteria() {
        Cafeteria newCafeteria = new Cafeteria();
        newCafeteria.setName("测试食堂_" + System.currentTimeMillis());
        newCafeteria.setLocation("测试位置");
        newCafeteria.setDescription("测试描述");
        newCafeteria.setLatitude(1.300);
        newCafeteria.setLongitude(103.770);

        EntityExchangeResult<byte[]> result = webTestClient.post()
                .uri("/api/cafeterias")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + adminToken)
                .bodyValue(newCafeteria)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.cafeteria.id").isNotEmpty()
                .jsonPath("$.cafeteria.name").isEqualTo(newCafeteria.getName())
                .returnResult();

        try {
            // 解析JSON响应获取ID
            String responseBody = new String(result.getResponseBody());
            Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
            Map<String, Object> cafeteriaData = (Map<String, Object>) response.get("cafeteria");
            return Long.valueOf(cafeteriaData.get("id").toString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse cafeteria ID from response", e);
        }
    }
}
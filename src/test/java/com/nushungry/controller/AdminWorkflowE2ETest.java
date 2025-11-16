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
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 管理员完整工作流端到端测试
 * 测试管理员登录后执行完整业务流程（不使用@PreAuthorize注解）
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AdminWorkflowE2ETest extends IntegrationTestBase {

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
    private User adminUser;
    private Cafeteria createdCafeteria;
    private Stall createdStall;

    @BeforeEach
    @Transactional
    void setUp() {
        // 创建管理员用户
        adminUser = new User();
        adminUser.setUsername("admin_e2e_test_" + System.currentTimeMillis());
        adminUser.setEmail("admin_e2e_" + System.currentTimeMillis() + "@test.com");
        adminUser.setPassword(passwordEncoder.encode("admin123"));
        adminUser.setRole(UserRole.ROLE_ADMIN);
        adminUser.setEnabled(true);
        adminUser = userRepository.save(adminUser);

        // 生成JWT token
        Map<String, Object> adminClaims = new HashMap<>();
        adminClaims.put("userId", adminUser.getId());
        adminClaims.put("role", UserRole.ROLE_ADMIN.getValue());
        adminToken = jwtUtil.generateAccessToken(adminUser.getUsername(), adminClaims);
    }

    @Test
    @DisplayName("管理员完整工作流测试 - 从登录到删除所有数据")
    void shouldCompleteAdminWorkflow_Successfully() {
        System.out.println("=== 开始管理员完整工作流测试 ===");

        // 步骤1: 管理员成功登录（通过token验证）
        assertAdminAuthenticationWorks();

        // 步骤2: 创建食堂
        createdCafeteria = createCafeteriaSuccessfully();
        assertNotNull(createdCafeteria.getId(), "食堂创建后应该有ID");
        System.out.println("✅ 食堂创建成功: " + createdCafeteria.getName());

        // 步骤3: 创建档口
        createdStall = createStallSuccessfully(createdCafeteria.getId());
        assertNotNull(createdStall.getId(), "档口创建后应该有ID");
        System.out.println("✅ 档口创建成功: " + createdStall.getName());

        // 步骤4: 修改食堂营业状态
        updateCafeteriaStatusSuccessfully(createdCafeteria.getId(), "CLOSED");
        System.out.println("✅ 食堂状态修改成功");

        // 步骤5: 删除档口
        deleteStallSuccessfully(createdStall.getId());
        System.out.println("✅ 档口删除成功");

        // 步骤6: 删除食堂
        deleteCafeteriaSuccessfully(createdCafeteria.getId());
        System.out.println("✅ 食堂删除成功");

        // 验证：确认数据已被删除
        verifyDataHasBeenDeleted();

        System.out.println("=== 管理员完整工作流测试完成 ===");
    }

    /**
     * 验证管理员认证
     */
    private void assertAdminAuthenticationWorks() {
        // 通过访问管理员接口来验证token有效性
        webTestClient.get()
                .uri("/api/cafeterias/admin?page=0&size=10")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true);

        System.out.println("✅ 管理员认证成功");
    }

    /**
     * 成功创建食堂
     */
    private Cafeteria createCafeteriaSuccessfully() {
        Map<String, Object> cafeteriaRequest = new HashMap<>();
        cafeteriaRequest.put("name", "E2E测试食堂_" + System.currentTimeMillis());
        cafeteriaRequest.put("location", "E2E测试位置");
        cafeteriaRequest.put("description", "E2E工作流测试食堂");
        cafeteriaRequest.put("latitude", 1.3000);
        cafeteriaRequest.put("longitude", 103.7700);
        cafeteriaRequest.put("termTimeOpeningHours", "08:00-22:00");
        cafeteriaRequest.put("vacationOpeningHours", "09:00-21:00");
        cafeteriaRequest.put("nearestBusStop", "E2E公交站");
        cafeteriaRequest.put("nearestCarpark", "E2E停车场");
        cafeteriaRequest.put("halalInfo", "HALAL");
        cafeteriaRequest.put("seatingCapacity", 200);

        var result = webTestClient.post()
                .uri("/api/cafeterias")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + adminToken)
                .bodyValue(cafeteriaRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.cafeteria.id").exists()
                .jsonPath("$.cafeteria.name").isEqualTo(cafeteriaRequest.get("name"))
                .returnResult();

        String responseBody = new String(result.getResponseBodyContent());
        System.out.println("创建食堂响应: " + responseBody);

        // 解析返回的食堂对象
        try {
            Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
            Map<String, Object> cafeteriaData = (Map<String, Object>) response.get("cafeteria");

            Cafeteria cafeteria = new Cafeteria();
            cafeteria.setId(((Number) cafeteriaData.get("id")).longValue());
            cafeteria.setName((String) cafeteriaData.get("name"));
            cafeteria.setLocation((String) cafeteriaData.get("location"));
            cafeteria.setDescription((String) cafeteriaData.get("description"));
            cafeteria.setLatitude(((Number) cafeteriaData.get("latitude")).doubleValue());
            cafeteria.setLongitude(((Number) cafeteriaData.get("longitude")).doubleValue());

            return cafeteria;
        } catch (Exception e) {
            throw new RuntimeException("解析食堂创建响应失败", e);
        }
    }

    /**
     * 成功创建档口
     */
    private Stall createStallSuccessfully(Long cafeteriaId) {
        Map<String, Object> stallRequest = new HashMap<>();
        stallRequest.put("name", "E2E测试档口_" + System.currentTimeMillis());
        stallRequest.put("cuisineType", "中餐");
        stallRequest.put("averagePrice", 15.0);
        stallRequest.put("halalInfo", "HALAL");
        stallRequest.put("contact", "123-456-7890");
        stallRequest.put("latitude", 1.301);
        stallRequest.put("longitude", 103.771);
        stallRequest.put("cafeteriaId", cafeteriaId);

        var result = webTestClient.post()
                .uri("/api/stalls")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + adminToken)
                .bodyValue(stallRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.stall.id").exists()
                .jsonPath("$.stall.name").isEqualTo(stallRequest.get("name"))
                .returnResult();

        String responseBody = new String(result.getResponseBodyContent());
        System.out.println("创建档口响应: " + responseBody);

        // 解析返回的档口对象
        try {
            Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
            Map<String, Object> stallData = (Map<String, Object>) response.get("stall");

            Stall stall = new Stall();
            stall.setId(((Number) stallData.get("id")).longValue());
            stall.setName((String) stallData.get("name"));
            stall.setCuisineType((String) stallData.get("cuisineType"));
            stall.setAveragePrice(((Number) stallData.get("averagePrice")).doubleValue());

            return stall;
        } catch (Exception e) {
            throw new RuntimeException("解析档口创建响应失败", e);
        }
    }

    /**
     * 成功修改食堂营业状态
     */
    private void updateCafeteriaStatusSuccessfully(Long cafeteriaId, String status) {
        Map<String, String> statusRequest = new HashMap<>();
        statusRequest.put("status", status);

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
                .jsonPath("$.status").isEqualTo(status);
    }

    /**
     * 成功删除档口
     */
    private void deleteStallSuccessfully(Long stallId) {
        webTestClient.delete()
                .uri("/api/stalls/" + stallId)
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.message").isEqualTo("档口删除成功");
    }

    /**
     * 成功删除食堂
     */
    private void deleteCafeteriaSuccessfully(Long cafeteriaId) {
        webTestClient.delete()
                .uri("/api/cafeterias/" + cafeteriaId)
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.message").isEqualTo("食堂删除成功");
    }

    /**
     * 验证数据已被删除
     */
    private void verifyDataHasBeenDeleted() {
        // 验证食堂已被删除 - 使用已创建的食堂ID
        if (createdCafeteria != null && createdCafeteria.getId() != null) {
            webTestClient.get()
                    .uri("/api/cafeterias/" + createdCafeteria.getId())
                    .exchange()
                    .expectStatus().isNotFound();
        }

        // 验证档口已被删除
        if (createdStall != null && createdStall.getId() != null) {
            webTestClient.get()
                    .uri("/api/stalls/" + createdStall.getId())
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }

    @Test
    @DisplayName("普通用户尝试管理员操作应该失败")
    void shouldFailRegularUserAttempts() {
        // 创建普通用户
        User regularUser = new User();
        regularUser.setUsername("regular_e2e_test_" + System.currentTimeMillis());
        regularUser.setEmail("regular_e2e_" + System.currentTimeMillis() + "@test.com");
        regularUser.setPassword(passwordEncoder.encode("user123"));
        regularUser.setRole(UserRole.ROLE_USER);
        regularUser.setEnabled(true);
        regularUser = userRepository.save(regularUser);

        // 生成普通用户token
        Map<String, Object> userClaims = new HashMap<>();
        userClaims.put("userId", regularUser.getId());
        userClaims.put("role", UserRole.ROLE_USER.getValue());
        String userToken = jwtUtil.generateAccessToken(regularUser.getUsername(), userClaims);

        // 普通用户尝试访问管理员列表接口 - 应该被SecurityConfig中的HTTP安全配置阻止
        webTestClient.get()
                .uri("/api/cafeterias/admin?page=0&size=10")
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isForbidden(); // 403 Forbidden

        System.out.println("✅ 普通用户权限验证正常 - 无法访问管理员接口");
    }

    @Test
    @DisplayName("未认证用户尝试管理员操作应该失败")
    void shouldFailUnauthenticatedAttempts() {
        // 未认证用户尝试访问管理员接口
        webTestClient.get()
                .uri("/api/cafeterias/admin?page=0&size=10")
                .exchange()
                .expectStatus().isForbidden(); // 403 Forbidden

        System.out.println("✅ 未认证用户权限验证正常 - 无法访问管理员接口");
    }
}
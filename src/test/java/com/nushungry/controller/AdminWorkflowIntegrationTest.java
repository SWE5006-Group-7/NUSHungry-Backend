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
 * 管理员完整工作流端到端集成测试
 * 测试管理员登录后执行完整业务流程
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AdminWorkflowIntegrationTest extends IntegrationTestBase {

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
        adminUser.setUsername("admin_workflow_test_" + System.currentTimeMillis());
        adminUser.setEmail("admin_workflow_" + System.currentTimeMillis() + "@test.com");
        adminUser.setPassword(passwordEncoder.encode("admin123"));
        adminUser.setRole(UserRole.ROLE_ADMIN);
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
        // 步骤1: 管理员成功登录（通过token验证）
        assertAdminAuthenticationWorks();

        // 步骤2: 创建食堂
        createdCafeteria = createCafeteriaSuccessfully();
        assertNotNull(createdCafeteria.getId(), "食堂创建后应该有ID");

        // 步骤3: 创建档口
        createdStall = createStallSuccessfully(createdCafeteria.getId());
        assertNotNull(createdStall.getId(), "档口创建后应该有ID");

        // 步骤4: 修改食堂营业状态
        updateCafeteriaStatusSuccessfully(createdCafeteria.getId(), "CLOSED");

        // 步骤5: 删除档口
        deleteStallSuccessfully(createdStall.getId());

        // 步骤6: 删除食堂
        deleteCafeteriaSuccessfully(createdCafeteria.getId());

        // 验证：确认数据已被删除
        verifyDataHasBeenDeleted();
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
    }

    /**
     * 成功创建食堂
     */
    private Cafeteria createCafeteriaSuccessfully() {
        Map<String, Object> cafeteriaRequest = new HashMap<>();
        cafeteriaRequest.put("name", "工作流测试食堂_" + System.currentTimeMillis());
        cafeteriaRequest.put("location", "测试位置_" + System.currentTimeMillis());
        cafeteriaRequest.put("description", "用于工作流测试的食堂");
        cafeteriaRequest.put("latitude", 1.3000);
        cafeteriaRequest.put("longitude", 103.7700);
        cafeteriaRequest.put("termTimeOpeningHours", "08:00-22:00");

        var response = webTestClient.post()
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

        // 从响应中提取cafeteria对象
        String responseBody = new String(response.getResponseBodyContent());
        try {
            com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(responseBody);
            com.fasterxml.jackson.databind.JsonNode cafeteriaNode = jsonNode.get("cafeteria");
            if (cafeteriaNode != null) {
                return objectMapper.treeToValue(cafeteriaNode, Cafeteria.class);
            }
        } catch (Exception e) {
            System.err.println("解析食堂响应失败: " + e.getMessage());
        }

        throw new RuntimeException("无法创建食堂");
    }

    /**
     * 成功创建档口
     */
    private Stall createStallSuccessfully(Long cafeteriaId) {
        Map<String, Object> stallRequest = new HashMap<>();
        stallRequest.put("name", "工作流测试档口_" + System.currentTimeMillis());
        stallRequest.put("cuisineType", "中餐");
        stallRequest.put("averagePrice", 15.0);
        stallRequest.put("cafeteriaId", cafeteriaId);

        var response = webTestClient.post()
                .uri("/api/stalls")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + adminToken)
                .bodyValue(stallRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.stall.id").exists()
                .returnResult();

        // 从响应中提取stall对象
        String responseBody = new String(response.getResponseBodyContent());
        try {
            com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(responseBody);
            com.fasterxml.jackson.databind.JsonNode stallNode = jsonNode.get("stall");
            if (stallNode != null) {
                return objectMapper.treeToValue(stallNode, Stall.class);
            }
        } catch (Exception e) {
            System.err.println("解析档口响应失败: " + e.getMessage());
        }

        throw new RuntimeException("无法创建档口");
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
        // 验证食堂已被删除（使用管理员token查询，应该返回404）
        webTestClient.get()
                .uri("/api/cafeterias/" + createdCafeteria.getId())
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody().isEmpty();
    }

    @Test
    @DisplayName("管理员工作流失败测试 - 普通用户尝试执行管理员操作")
    void shouldFailWorkflow_WhenRegularUserAttempts() {
        // 创建普通用户和token
        User regularUser = createRegularUser();
        String regularToken = generateRegularUserToken(regularUser);

        // 普通用户尝试创建食堂 - 应该失败
        Cafeteria newCafeteria = new Cafeteria();
        newCafeteria.setName("普通用户创建的食堂");
        newCafeteria.setLocation("普通用户位置");

        webTestClient.post()
                .uri("/api/cafeterias")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + regularToken)
                .bodyValue(newCafeteria)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("管理员工作流失败测试 - 未认证用户尝试执行操作")
    void shouldFailWorkflow_WhenUnauthenticatedUserAttempts() {
        // 未认证用户尝试创建食堂 - 应该失败
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

    /**
     * 创建普通用户辅助方法
     */
    private User createRegularUser() {
        User regularUser = new User();
        regularUser.setUsername("regular_user_" + System.currentTimeMillis());
        regularUser.setEmail("regular_" + System.currentTimeMillis() + "@test.com");
        regularUser.setPassword(passwordEncoder.encode("user123"));
        regularUser.setRole(UserRole.ROLE_USER);
        return userRepository.save(regularUser);
    }

    /**
     * 生成普通用户token辅助方法
     */
    private String generateRegularUserToken(User user) {
        Map<String, Object> userClaims = new HashMap<>();
        userClaims.put("userId", user.getId());
        userClaims.put("role", UserRole.ROLE_USER.getValue());
        return jwtUtil.generateAccessToken(user.getUsername(), userClaims);
    }
}
package com.nushungry.repository;

import com.nushungry.model.User;
import com.nushungry.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserRepository 测试类
 * 重点测试 @Query 注解的自定义批量更新查询
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository 自定义查询测试")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        // 清空数据库
        userRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        // 创建测试用户
        user1 = createUser("user1", "user1@test.com", UserRole.ROLE_USER, true);
        user2 = createUser("user2", "user2@test.com", UserRole.ROLE_USER, true);
        user3 = createUser("user3", "user3@test.com", UserRole.ROLE_ADMIN, false);

        user1 = entityManager.persist(user1);
        user2 = entityManager.persist(user2);
        user3 = entityManager.persist(user3);
        entityManager.flush();
    }

    @Test
    @DisplayName("批量更新用户启用状态 - 正常场景")
    void testUpdateEnabledStatusByIds_Success() {
        // Given: 两个启用的用户
        List<Long> ids = Arrays.asList(user1.getId(), user2.getId());
        assertThat(user1.getEnabled()).isTrue();
        assertThat(user2.getEnabled()).isTrue();

        // When: 批量禁用这两个用户
        int updatedCount = userRepository.updateEnabledStatusByIds(ids, false);
        entityManager.flush();
        entityManager.clear();

        // Then: 返回值应为2，用户状态应更新为禁用
        assertThat(updatedCount).isEqualTo(2);

        User updatedUser1 = userRepository.findById(user1.getId()).orElseThrow();
        User updatedUser2 = userRepository.findById(user2.getId()).orElseThrow();
        User unchangedUser3 = userRepository.findById(user3.getId()).orElseThrow();

        assertThat(updatedUser1.getEnabled()).isFalse();
        assertThat(updatedUser2.getEnabled()).isFalse();
        assertThat(unchangedUser3.getEnabled()).isFalse(); // 未被更新
    }

    @Test
    @DisplayName("批量更新用户启用状态 - 启用禁用的用户")
    void testUpdateEnabledStatusByIds_EnableDisabledUsers() {
        // Given: 一个禁用的用户
        List<Long> ids = List.of(user3.getId());
        assertThat(user3.getEnabled()).isFalse();

        // When: 启用该用户
        int updatedCount = userRepository.updateEnabledStatusByIds(ids, true);
        entityManager.flush();
        entityManager.clear();

        // Then: 返回值应为1，用户应被启用
        assertThat(updatedCount).isEqualTo(1);

        User updatedUser3 = userRepository.findById(user3.getId()).orElseThrow();
        assertThat(updatedUser3.getEnabled()).isTrue();
    }

    @Test
    @DisplayName("批量更新用户启用状态 - 空ID列表")
    void testUpdateEnabledStatusByIds_EmptyList() {
        // Given: 空ID列表
        List<Long> emptyIds = List.of();

        // When: 执行批量更新
        int updatedCount = userRepository.updateEnabledStatusByIds(emptyIds, false);

        // Then: 返回值应为0，没有用户被更新
        assertThat(updatedCount).isEqualTo(0);

        // 验证所有用户状态未改变
        User unchangedUser1 = userRepository.findById(user1.getId()).orElseThrow();
        User unchangedUser2 = userRepository.findById(user2.getId()).orElseThrow();
        assertThat(unchangedUser1.getEnabled()).isTrue();
        assertThat(unchangedUser2.getEnabled()).isTrue();
    }

    @Test
    @DisplayName("批量更新用户启用状态 - 不存在的ID")
    void testUpdateEnabledStatusByIds_NonExistentIds() {
        // Given: 不存在的ID列表
        List<Long> nonExistentIds = Arrays.asList(9999L, 10000L);

        // When: 执行批量更新
        int updatedCount = userRepository.updateEnabledStatusByIds(nonExistentIds, false);

        // Then: 返回值应为0，因为ID不存在
        assertThat(updatedCount).isEqualTo(0);
    }

    @Test
    @DisplayName("批量更新用户启用状态 - 混合存在和不存在的ID")
    void testUpdateEnabledStatusByIds_MixedIds() {
        // Given: 包含存在和不存在ID的列表
        List<Long> mixedIds = Arrays.asList(user1.getId(), 9999L);

        // When: 执行批量更新
        int updatedCount = userRepository.updateEnabledStatusByIds(mixedIds, false);
        entityManager.flush();
        entityManager.clear();

        // Then: 返回值应为1（只更新了存在的用户）
        assertThat(updatedCount).isEqualTo(1);

        User updatedUser1 = userRepository.findById(user1.getId()).orElseThrow();
        assertThat(updatedUser1.getEnabled()).isFalse();
    }

    @Test
    @DisplayName("批量更新用户启用状态 - 验证事务性（幂等性）")
    void testUpdateEnabledStatusByIds_Idempotency() {
        // Given: 已禁用的用户
        List<Long> ids = List.of(user1.getId());
        userRepository.updateEnabledStatusByIds(ids, false);
        entityManager.flush();
        entityManager.clear();

        // When: 再次禁用同一用户
        int updatedCount = userRepository.updateEnabledStatusByIds(ids, false);
        entityManager.flush();
        entityManager.clear();

        // Then: 返回值仍为1（UPDATE语句执行了，即使值未改变）
        assertThat(updatedCount).isEqualTo(1);

        User user = userRepository.findById(user1.getId()).orElseThrow();
        assertThat(user.getEnabled()).isFalse();
    }

    @Test
    @DisplayName("批量更新用户启用状态 - 大量ID批量操作")
    void testUpdateEnabledStatusByIds_LargeBatch() {
        // Given: 创建10个用户
        for (int i = 0; i < 10; i++) {
            User user = createUser("bulkUser" + i, "bulkUser" + i + "@test.com", UserRole.ROLE_USER, true);
            entityManager.persist(user);
        }
        entityManager.flush();
        entityManager.clear();

        List<User> allUsers = userRepository.findAll();
        List<Long> allIds = allUsers.stream().map(User::getId).toList();

        // When: 批量禁用所有用户
        int updatedCount = userRepository.updateEnabledStatusByIds(allIds, false);
        entityManager.flush();
        entityManager.clear();

        // Then: 所有用户应被禁用
        assertThat(updatedCount).isEqualTo(allUsers.size());

        List<User> disabledUsers = userRepository.findAll();
        assertThat(disabledUsers).allMatch(user -> !user.getEnabled());
    }

    // ==================== 辅助方法 ====================

    private User createUser(String username, String email, UserRole role, boolean enabled) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password123");
        user.setRole(role);
        user.setEnabled(enabled);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }
}

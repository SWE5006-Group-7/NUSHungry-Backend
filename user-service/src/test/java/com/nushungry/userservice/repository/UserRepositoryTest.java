package com.nushungry.userservice.repository;

import com.nushungry.userservice.model.User;
import com.nushungry.userservice.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserRepository 单元测试
 * 使用 @DataJpaTest 和 H2 内存数据库
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository 测试")
@Import(com.nushungry.userservice.config.JpaConfig.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        entityManager.clear();
    }

    @Test
    @DisplayName("findByEmail - 应该根据邮箱查找用户")
    void testFindByEmail_Success() {
        // Given
        User user = createUser("testuser", "test@example.com", UserRole.ROLE_USER);
        userRepository.save(user);
        entityManager.flush();
        entityManager.clear();

        // When
        Optional<User> found = userRepository.findByEmail("test@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        assertThat(found.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("findByEmail - 邮箱不存在时应返回空")
    void testFindByEmail_NotFound() {
        // When
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findByUsername - 应该根据用户名查找用户")
    void testFindByUsername_Success() {
        // Given
        User user = createUser("testuser", "test@example.com", UserRole.ROLE_USER);
        userRepository.save(user);
        entityManager.flush();
        entityManager.clear();

        // When
        Optional<User> found = userRepository.findByUsername("testuser");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("testuser");
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("findByUsername - 用户名不存在时应返回空")
    void testFindByUsername_NotFound() {
        // When
        Optional<User> found = userRepository.findByUsername("nonexistent");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("existsByEmail - 邮箱存在时应返回true")
    void testExistsByEmail_True() {
        // Given
        User user = createUser("testuser", "test@example.com", UserRole.ROLE_USER);
        userRepository.save(user);
        entityManager.flush();

        // When
        boolean exists = userRepository.existsByEmail("test@example.com");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByEmail - 邮箱不存在时应返回false")
    void testExistsByEmail_False() {
        // When
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("existsByUsername - 用户名存在时应返回true")
    void testExistsByUsername_True() {
        // Given
        User user = createUser("testuser", "test@example.com", UserRole.ROLE_USER);
        userRepository.save(user);
        entityManager.flush();

        // When
        boolean exists = userRepository.existsByUsername("testuser");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByUsername - 用户名不存在时应返回false")
    void testExistsByUsername_False() {
        // When
        boolean exists = userRepository.existsByUsername("nonexistent");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("countByCreatedAtBefore - 应该统计指定时间之前创建的用户数")
    void testCountByCreatedAtBefore() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime past = now.minusDays(2);

        User user1 = createUser("user1", "user1@example.com", UserRole.ROLE_USER);
        User user2 = createUser("user2", "user2@example.com", UserRole.ROLE_USER);
        User user3 = createUser("user3", "user3@example.com", UserRole.ROLE_USER);

        userRepository.save(user1);
        userRepository.save(user2);
        userRepository.save(user3);
        entityManager.flush();

        // 手动更新createdAt字段 (因为H2测试环境中JPA审计可能不生效)
        entityManager.getEntityManager()
                .createQuery("UPDATE User u SET u.createdAt = :time WHERE u.id = :id")
                .setParameter("time", past.minusDays(1))
                .setParameter("id", user1.getId())
                .executeUpdate();

        entityManager.getEntityManager()
                .createQuery("UPDATE User u SET u.createdAt = :time WHERE u.id = :id")
                .setParameter("time", past.plusDays(1))
                .setParameter("id", user2.getId())
                .executeUpdate();

        entityManager.getEntityManager()
                .createQuery("UPDATE User u SET u.createdAt = :time WHERE u.id = :id")
                .setParameter("time", now.plusDays(1))
                .setParameter("id", user3.getId())
                .executeUpdate();

        entityManager.flush();
        entityManager.clear();

        // When
        long count = userRepository.countByCreatedAtBefore(now);

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("countByCreatedAtBetween - 应该统计指定时间范围内创建的用户数")
    void testCountByCreatedAtBetween() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusDays(7);
        LocalDateTime end = now.minusDays(1);

        User user1 = createUser("user1", "user1@example.com", UserRole.ROLE_USER);
        User user2 = createUser("user2", "user2@example.com", UserRole.ROLE_USER);
        User user3 = createUser("user3", "user3@example.com", UserRole.ROLE_USER);
        User user4 = createUser("user4", "user4@example.com", UserRole.ROLE_USER);

        userRepository.save(user1);
        userRepository.save(user2);
        userRepository.save(user3);
        userRepository.save(user4);
        entityManager.flush();

        // 手动更新createdAt字段
        entityManager.getEntityManager()
                .createQuery("UPDATE User u SET u.createdAt = :time WHERE u.id = :id")
                .setParameter("time", start.minusDays(1)) // 太早
                .setParameter("id", user1.getId())
                .executeUpdate();

        entityManager.getEntityManager()
                .createQuery("UPDATE User u SET u.createdAt = :time WHERE u.id = :id")
                .setParameter("time", start.plusDays(2)) // 在范围内
                .setParameter("id", user2.getId())
                .executeUpdate();

        entityManager.getEntityManager()
                .createQuery("UPDATE User u SET u.createdAt = :time WHERE u.id = :id")
                .setParameter("time", end.minusHours(1)) // 在范围内
                .setParameter("id", user3.getId())
                .executeUpdate();

        entityManager.getEntityManager()
                .createQuery("UPDATE User u SET u.createdAt = :time WHERE u.id = :id")
                .setParameter("time", end.plusDays(1)) // 太晚
                .setParameter("id", user4.getId())
                .executeUpdate();

        entityManager.flush();
        entityManager.clear();

        // When
        long count = userRepository.countByCreatedAtBetween(start, end);

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("countByLastLoginAfter - 应该统计指定时间之后登录的用户数")
    void testCountByLastLoginAfter() {
        // Given
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);

        User user1 = createUser("user1", "user1@example.com", UserRole.ROLE_USER);
        user1.setLastLogin(cutoff.minusDays(1)); // 太早

        User user2 = createUser("user2", "user2@example.com", UserRole.ROLE_USER);
        user2.setLastLogin(cutoff.plusDays(1)); // 活跃

        User user3 = createUser("user3", "user3@example.com", UserRole.ROLE_USER);
        user3.setLastLogin(LocalDateTime.now()); // 活跃

        User user4 = createUser("user4", "user4@example.com", UserRole.ROLE_USER);
        // user4 没有lastLogin (null)

        userRepository.save(user1);
        userRepository.save(user2);
        userRepository.save(user3);
        userRepository.save(user4);
        entityManager.flush();

        // When
        long count = userRepository.countByLastLoginAfter(cutoff);

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("save - 应该保存用户并自动填充时间戳")
    void testSave_AutoPopulateTimestamps() {
        // Given
        User user = createUser("testuser", "test@example.com", UserRole.ROLE_USER);

        // When
        User saved = userRepository.save(user);
        entityManager.flush();

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("save - 更新用户时应该更新 updatedAt")
    void testSave_UpdatesUpdatedAt() throws InterruptedException {
        // Given
        User user = createUser("testuser", "test@example.com", UserRole.ROLE_USER);
        User saved = userRepository.save(user);
        entityManager.flush();
        LocalDateTime originalUpdatedAt = saved.getUpdatedAt();

        // Wait a bit to ensure timestamp difference
        Thread.sleep(100);

        // When
        saved.setEmail("newemail@example.com");
        User updated = userRepository.save(saved);
        entityManager.flush();

        // Then
        assertThat(updated.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    @DisplayName("邮箱唯一性约束测试 - 应该允许不同用户使用不同邮箱")
    void testEmailUniqueness_DifferentEmails() {
        // Given
        User user1 = createUser("user1", "user1@example.com", UserRole.ROLE_USER);
        User user2 = createUser("user2", "user2@example.com", UserRole.ROLE_USER);

        // When
        userRepository.save(user1);
        userRepository.save(user2);
        entityManager.flush();

        // Then
        assertThat(userRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("用户名唯一性约束测试 - 应该允许不同用户使用不同用户名")
    void testUsernameUniqueness_DifferentUsernames() {
        // Given
        User user1 = createUser("user1", "test1@example.com", UserRole.ROLE_USER);
        User user2 = createUser("user2", "test2@example.com", UserRole.ROLE_USER);

        // When
        userRepository.save(user1);
        userRepository.save(user2);
        entityManager.flush();

        // Then
        assertThat(userRepository.count()).isEqualTo(2);
    }

    /**
     * 创建测试用户的辅助方法
     */
    private User createUser(String username, String email, UserRole role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("encodedPassword123");
        user.setRole(role);
        user.setEnabled(true);
        return user;
    }
}

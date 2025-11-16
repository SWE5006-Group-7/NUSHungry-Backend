package com.nushungry.userservice.repository;

import com.nushungry.userservice.model.RefreshToken;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RefreshTokenRepository 单元测试
 * 使用 @DataJpaTest 和 H2 内存数据库
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("RefreshTokenRepository 测试")
@Import(com.nushungry.userservice.config.JpaConfig.class)
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser1;
    private User testUser2;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        entityManager.clear();

        // 创建测试用户
        testUser1 = createUser("user1", "user1@example.com");
        testUser2 = createUser("user2", "user2@example.com");
        testUser1 = userRepository.save(testUser1);
        testUser2 = userRepository.save(testUser2);
        entityManager.flush();
    }

    @Test
    @DisplayName("findByToken - 应该根据token查找RefreshToken")
    void testFindByToken_Success() {
        // Given
        RefreshToken token = createRefreshToken(testUser1, "valid-token-123");
        refreshTokenRepository.save(token);
        entityManager.flush();
        entityManager.clear();

        // When
        Optional<RefreshToken> found = refreshTokenRepository.findByToken("valid-token-123");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getToken()).isEqualTo("valid-token-123");
        assertThat(found.get().getUser().getId()).isEqualTo(testUser1.getId());
    }

    @Test
    @DisplayName("findByToken - token不存在时应返回空")
    void testFindByToken_NotFound() {
        // When
        Optional<RefreshToken> found = refreshTokenRepository.findByToken("nonexistent-token");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findByUser - 应该查找用户的所有RefreshToken")
    void testFindByUser_MultipleTokens() {
        // Given
        RefreshToken token1 = createRefreshToken(testUser1, "token-1");
        RefreshToken token2 = createRefreshToken(testUser1, "token-2");
        RefreshToken token3 = createRefreshToken(testUser2, "token-3");

        refreshTokenRepository.save(token1);
        refreshTokenRepository.save(token2);
        refreshTokenRepository.save(token3);
        entityManager.flush();
        entityManager.clear();

        // When
        List<RefreshToken> user1Tokens = refreshTokenRepository.findByUser(testUser1);
        List<RefreshToken> user2Tokens = refreshTokenRepository.findByUser(testUser2);

        // Then
        assertThat(user1Tokens).hasSize(2);
        assertThat(user1Tokens)
                .extracting(RefreshToken::getToken)
                .containsExactlyInAnyOrder("token-1", "token-2");

        assertThat(user2Tokens).hasSize(1);
        assertThat(user2Tokens.get(0).getToken()).isEqualTo("token-3");
    }

    @Test
    @DisplayName("findByUser - 用户没有token时应返回空列表")
    void testFindByUser_EmptyList() {
        // When
        List<RefreshToken> tokens = refreshTokenRepository.findByUser(testUser1);

        // Then
        assertThat(tokens).isEmpty();
    }

    @Test
    @DisplayName("deleteByUser - 应该删除用户的所有token")
    void testDeleteByUser() {
        // Given
        RefreshToken token1 = createRefreshToken(testUser1, "token-1");
        RefreshToken token2 = createRefreshToken(testUser1, "token-2");
        RefreshToken token3 = createRefreshToken(testUser2, "token-3");

        refreshTokenRepository.save(token1);
        refreshTokenRepository.save(token2);
        refreshTokenRepository.save(token3);
        entityManager.flush();
        entityManager.clear();

        // When
        refreshTokenRepository.deleteByUser(testUser1);
        entityManager.flush();
        entityManager.clear();

        // Then
        List<RefreshToken> user1Tokens = refreshTokenRepository.findByUser(testUser1);
        List<RefreshToken> user2Tokens = refreshTokenRepository.findByUser(testUser2);

        assertThat(user1Tokens).isEmpty();
        assertThat(user2Tokens).hasSize(1);
    }

    @Test
    @DisplayName("deleteExpiredTokens - 应该删除所有过期的token")
    void testDeleteExpiredTokens() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        RefreshToken expiredToken1 = createRefreshToken(testUser1, "expired-1");
        expiredToken1.setExpiryDate(now.minusDays(1));

        RefreshToken expiredToken2 = createRefreshToken(testUser2, "expired-2");
        expiredToken2.setExpiryDate(now.minusHours(1));

        RefreshToken validToken = createRefreshToken(testUser1, "valid-token");
        validToken.setExpiryDate(now.plusDays(1));

        refreshTokenRepository.save(expiredToken1);
        refreshTokenRepository.save(expiredToken2);
        refreshTokenRepository.save(validToken);
        entityManager.flush();
        entityManager.clear();

        // When
        int deleted = refreshTokenRepository.deleteExpiredTokens(now);
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(deleted).isEqualTo(2);
        assertThat(refreshTokenRepository.count()).isEqualTo(1);
        assertThat(refreshTokenRepository.findByToken("valid-token")).isPresent();
    }

    @Test
    @DisplayName("deleteExpiredTokens - 没有过期token时应返回0")
    void testDeleteExpiredTokens_NoExpired() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        RefreshToken validToken1 = createRefreshToken(testUser1, "valid-1");
        validToken1.setExpiryDate(now.plusDays(1));

        RefreshToken validToken2 = createRefreshToken(testUser2, "valid-2");
        validToken2.setExpiryDate(now.plusHours(1));

        refreshTokenRepository.save(validToken1);
        refreshTokenRepository.save(validToken2);
        entityManager.flush();

        // When
        int deleted = refreshTokenRepository.deleteExpiredTokens(now);

        // Then
        assertThat(deleted).isEqualTo(0);
        assertThat(refreshTokenRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("revokeAllUserTokens - 应该撤销用户的所有token")
    void testRevokeAllUserTokens() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        RefreshToken token1 = createRefreshToken(testUser1, "token-1");
        RefreshToken token2 = createRefreshToken(testUser1, "token-2");
        RefreshToken token3 = createRefreshToken(testUser2, "token-3");

        refreshTokenRepository.save(token1);
        refreshTokenRepository.save(token2);
        refreshTokenRepository.save(token3);
        entityManager.flush();
        entityManager.clear();

        // When
        int revoked = refreshTokenRepository.revokeAllUserTokens(testUser1, now);
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(revoked).isEqualTo(2);

        List<RefreshToken> user1Tokens = refreshTokenRepository.findByUser(testUser1);
        assertThat(user1Tokens).hasSize(2);
        assertThat(user1Tokens).allMatch(RefreshToken::getRevoked);

        List<RefreshToken> user2Tokens = refreshTokenRepository.findByUser(testUser2);
        assertThat(user2Tokens).hasSize(1);
        assertThat(user2Tokens.get(0).getRevoked()).isFalse();
    }

    @Test
    @DisplayName("findValidTokensByUser - 应该只查找有效的token (未过期且未撤销)")
    void testFindValidTokensByUser() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // 有效token
        RefreshToken validToken = createRefreshToken(testUser1, "valid");
        validToken.setExpiryDate(now.plusDays(1));
        validToken.setRevoked(false);

        // 过期token
        RefreshToken expiredToken = createRefreshToken(testUser1, "expired");
        expiredToken.setExpiryDate(now.minusDays(1));
        expiredToken.setRevoked(false);

        // 已撤销token
        RefreshToken revokedToken = createRefreshToken(testUser1, "revoked");
        revokedToken.setExpiryDate(now.plusDays(1));
        revokedToken.setRevoked(true);

        // 已过期且已撤销
        RefreshToken expiredAndRevoked = createRefreshToken(testUser1, "expired-revoked");
        expiredAndRevoked.setExpiryDate(now.minusDays(1));
        expiredAndRevoked.setRevoked(true);

        refreshTokenRepository.save(validToken);
        refreshTokenRepository.save(expiredToken);
        refreshTokenRepository.save(revokedToken);
        refreshTokenRepository.save(expiredAndRevoked);
        entityManager.flush();
        entityManager.clear();

        // When
        List<RefreshToken> validTokens = refreshTokenRepository.findValidTokensByUser(testUser1, now);

        // Then
        assertThat(validTokens).hasSize(1);
        assertThat(validTokens.get(0).getToken()).isEqualTo("valid");
    }

    @Test
    @DisplayName("findValidTokensByUser - 用户没有有效token时应返回空列表")
    void testFindValidTokensByUser_NoValid() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        RefreshToken expiredToken = createRefreshToken(testUser1, "expired");
        expiredToken.setExpiryDate(now.minusDays(1));

        refreshTokenRepository.save(expiredToken);
        entityManager.flush();

        // When
        List<RefreshToken> validTokens = refreshTokenRepository.findValidTokensByUser(testUser1, now);

        // Then
        assertThat(validTokens).isEmpty();
    }

    @Test
    @DisplayName("save - 应该保存token并自动填充时间戳")
    void testSave_AutoPopulateTimestamps() {
        // Given
        RefreshToken token = createRefreshToken(testUser1, "test-token");

        // When
        RefreshToken saved = refreshTokenRepository.save(token);
        entityManager.flush();

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("token唯一性约束测试 - 不同用户可以有不同token")
    void testTokenUniqueness() {
        // Given
        RefreshToken token1 = createRefreshToken(testUser1, "unique-token-1");
        RefreshToken token2 = createRefreshToken(testUser2, "unique-token-2");

        // When
        refreshTokenRepository.save(token1);
        refreshTokenRepository.save(token2);
        entityManager.flush();

        // Then
        assertThat(refreshTokenRepository.count()).isEqualTo(2);
    }

    /**
     * 创建RefreshToken的辅助方法
     */
    private RefreshToken createRefreshToken(User user, String tokenValue) {
        RefreshToken token = new RefreshToken();
        token.setToken(tokenValue);
        token.setUser(user);
        token.setExpiryDate(LocalDateTime.now().plusDays(30));
        token.setRevoked(false);
        return token;
    }

    /**
     * 创建User的辅助方法
     */
    private User createUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("encodedPassword123");
        user.setRole(UserRole.ROLE_USER);
        user.setEnabled(true);
        return user;
    }
}

package com.nushungry.userservice.repository;

import com.nushungry.userservice.model.VerificationCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VerificationCodeRepository 单元测试
 * 使用 @DataJpaTest 和 H2 内存数据库
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("VerificationCodeRepository 测试")
class VerificationCodeRepositoryTest {

    @Autowired
    private VerificationCodeRepository verificationCodeRepository;

    @Autowired
    private TestEntityManager entityManager;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TYPE_PASSWORD_RESET = "PASSWORD_RESET";
    private static final String TYPE_EMAIL_VERIFY = "EMAIL_VERIFY";

    @BeforeEach
    void setUp() {
        verificationCodeRepository.deleteAll();
        entityManager.clear();
    }

    @Test
    @DisplayName("findLatestValidCode - 应该返回最新的有效验证码")
    void testFindLatestValidCode_Success() throws InterruptedException {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // 旧验证码 - 使用明确较早的createdAt时间
        VerificationCode oldCode = new VerificationCode();
        oldCode.setEmail(TEST_EMAIL);
        oldCode.setCode("123456");
        oldCode.setType(TYPE_PASSWORD_RESET);
        oldCode.setCreatedAt(now.minusMinutes(10));
        oldCode.setExpiresAt(now.plusHours(1));
        oldCode.setUsed(false);
        oldCode.setIpAddress("127.0.0.1");

        verificationCodeRepository.save(oldCode);
        entityManager.flush();

        // 等待一小段时间确保时间戳不同
        Thread.sleep(10);

        // 最新验证码 - 使用明确较晚的createdAt时间
        VerificationCode latestCode = new VerificationCode();
        latestCode.setEmail(TEST_EMAIL);
        latestCode.setCode("654321");
        latestCode.setType(TYPE_PASSWORD_RESET);
        latestCode.setCreatedAt(now.minusMinutes(5));
        latestCode.setExpiresAt(now.plusHours(1));
        latestCode.setUsed(false);
        latestCode.setIpAddress("127.0.0.1");

        verificationCodeRepository.save(latestCode);
        entityManager.flush();
        entityManager.clear();

        // When
        Optional<VerificationCode> found = verificationCodeRepository.findLatestValidCode(TEST_EMAIL, TYPE_PASSWORD_RESET, now);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getCode()).isEqualTo("654321");
    }

    @Test
    @DisplayName("findLatestValidCode - 过期验证码应该被过滤")
    void testFindLatestValidCode_FilterExpired() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // 已过期验证码
        VerificationCode expiredCode = createVerificationCode(TEST_EMAIL, "123456", TYPE_PASSWORD_RESET, now.minusHours(1));
        expiredCode.setCreatedAt(now.minusHours(2));

        verificationCodeRepository.save(expiredCode);
        entityManager.flush();

        // When
        Optional<VerificationCode> found = verificationCodeRepository.findLatestValidCode(TEST_EMAIL, TYPE_PASSWORD_RESET, now);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findLatestValidCode - 已使用验证码应该被过滤")
    void testFindLatestValidCode_FilterUsed() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // 已使用验证码
        VerificationCode usedCode = createVerificationCode(TEST_EMAIL, "123456", TYPE_PASSWORD_RESET, now.plusHours(1));
        usedCode.setUsed(true);
        usedCode.setUsedAt(now.minusMinutes(5));

        verificationCodeRepository.save(usedCode);
        entityManager.flush();

        // When
        Optional<VerificationCode> found = verificationCodeRepository.findLatestValidCode(TEST_EMAIL, TYPE_PASSWORD_RESET, now);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findLatestValidCode - 应该区分不同类型的验证码")
    void testFindLatestValidCode_DifferentTypes() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        VerificationCode passwordResetCode = createVerificationCode(TEST_EMAIL, "123456", TYPE_PASSWORD_RESET, now.plusHours(1));
        VerificationCode emailVerifyCode = createVerificationCode(TEST_EMAIL, "654321", TYPE_EMAIL_VERIFY, now.plusHours(1));

        verificationCodeRepository.save(passwordResetCode);
        verificationCodeRepository.save(emailVerifyCode);
        entityManager.flush();

        // When
        Optional<VerificationCode> passwordReset = verificationCodeRepository.findLatestValidCode(TEST_EMAIL, TYPE_PASSWORD_RESET, now);
        Optional<VerificationCode> emailVerify = verificationCodeRepository.findLatestValidCode(TEST_EMAIL, TYPE_EMAIL_VERIFY, now);

        // Then
        assertThat(passwordReset).isPresent();
        assertThat(passwordReset.get().getCode()).isEqualTo("123456");

        assertThat(emailVerify).isPresent();
        assertThat(emailVerify.get().getCode()).isEqualTo("654321");
    }

    @Test
    @DisplayName("findByEmail - 应该返回指定邮箱的所有验证码")
    void testFindByEmail() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        String email1 = "user1@example.com";
        String email2 = "user2@example.com";

        verificationCodeRepository.save(createVerificationCode(email1, "111111", TYPE_PASSWORD_RESET, now.plusHours(1)));
        verificationCodeRepository.save(createVerificationCode(email1, "222222", TYPE_EMAIL_VERIFY, now.plusHours(1)));
        verificationCodeRepository.save(createVerificationCode(email2, "333333", TYPE_PASSWORD_RESET, now.plusHours(1)));
        entityManager.flush();

        // When
        List<VerificationCode> email1Codes = verificationCodeRepository.findByEmail(email1);
        List<VerificationCode> email2Codes = verificationCodeRepository.findByEmail(email2);

        // Then
        assertThat(email1Codes).hasSize(2);
        assertThat(email1Codes)
                .extracting(VerificationCode::getCode)
                .containsExactlyInAnyOrder("111111", "222222");

        assertThat(email2Codes).hasSize(1);
        assertThat(email2Codes.get(0).getCode()).isEqualTo("333333");
    }

    @Test
    @DisplayName("findByEmail - 邮箱没有验证码时应返回空列表")
    void testFindByEmail_Empty() {
        // When
        List<VerificationCode> codes = verificationCodeRepository.findByEmail("nonexistent@example.com");

        // Then
        assertThat(codes).isEmpty();
    }

    @Test
    @DisplayName("deleteExpiredCodes - 应该删除所有过期的验证码")
    void testDeleteExpiredCodes() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // 过期验证码
        verificationCodeRepository.save(createVerificationCode(TEST_EMAIL, "111111", TYPE_PASSWORD_RESET, now.minusHours(1)));
        verificationCodeRepository.save(createVerificationCode("user2@example.com", "222222", TYPE_EMAIL_VERIFY, now.minusMinutes(1)));

        // 有效验证码
        verificationCodeRepository.save(createVerificationCode("user3@example.com", "333333", TYPE_PASSWORD_RESET, now.plusHours(1)));

        entityManager.flush();
        entityManager.clear();

        // When
        verificationCodeRepository.deleteExpiredCodes(now);
        entityManager.flush();

        // Then
        assertThat(verificationCodeRepository.count()).isEqualTo(1);
        List<VerificationCode> remaining = verificationCodeRepository.findAll();
        assertThat(remaining.get(0).getCode()).isEqualTo("333333");
    }

    @Test
    @DisplayName("deleteExpiredCodes - 没有过期验证码时不应删除任何记录")
    void testDeleteExpiredCodes_NoExpired() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        verificationCodeRepository.save(createVerificationCode(TEST_EMAIL, "111111", TYPE_PASSWORD_RESET, now.plusHours(1)));
        verificationCodeRepository.save(createVerificationCode("user2@example.com", "222222", TYPE_EMAIL_VERIFY, now.plusHours(2)));

        entityManager.flush();
        long countBefore = verificationCodeRepository.count();

        // When
        verificationCodeRepository.deleteExpiredCodes(now);
        entityManager.flush();

        // Then
        assertThat(verificationCodeRepository.count()).isEqualTo(countBefore);
    }

    @Test
    @DisplayName("countRecentCodes - 应该统计指定时间段内的验证码数量")
    void testCountRecentCodes() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since = now.minusMinutes(10);

        // 时间范围内的验证码
        VerificationCode recentCode1 = createVerificationCode(TEST_EMAIL, "111111", TYPE_PASSWORD_RESET, now.plusHours(1));
        recentCode1.setCreatedAt(now.minusMinutes(5));

        VerificationCode recentCode2 = createVerificationCode(TEST_EMAIL, "222222", TYPE_PASSWORD_RESET, now.plusHours(1));
        recentCode2.setCreatedAt(now.minusMinutes(3));

        // 时间范围外的验证码
        VerificationCode oldCode = createVerificationCode(TEST_EMAIL, "333333", TYPE_PASSWORD_RESET, now.plusHours(1));
        oldCode.setCreatedAt(now.minusMinutes(15));

        // 不同类型的验证码（不应计入）
        VerificationCode differentType = createVerificationCode(TEST_EMAIL, "444444", TYPE_EMAIL_VERIFY, now.plusHours(1));
        differentType.setCreatedAt(now.minusMinutes(5));

        verificationCodeRepository.save(recentCode1);
        verificationCodeRepository.save(recentCode2);
        verificationCodeRepository.save(oldCode);
        verificationCodeRepository.save(differentType);
        entityManager.flush();

        // When
        long count = verificationCodeRepository.countRecentCodes(TEST_EMAIL, TYPE_PASSWORD_RESET, since);

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("countRecentCodes - 没有符合条件的验证码时应返回0")
    void testCountRecentCodes_NoRecent() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since = now.minusMinutes(5);

        VerificationCode oldCode = createVerificationCode(TEST_EMAIL, "111111", TYPE_PASSWORD_RESET, now.plusHours(1));
        oldCode.setCreatedAt(now.minusMinutes(10));

        verificationCodeRepository.save(oldCode);
        entityManager.flush();

        // When
        long count = verificationCodeRepository.countRecentCodes(TEST_EMAIL, TYPE_PASSWORD_RESET, since);

        // Then
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("markAsUsed - 应该标记验证码为已使用")
    void testMarkAsUsed() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        VerificationCode code = createVerificationCode(TEST_EMAIL, "123456", TYPE_PASSWORD_RESET, now.plusHours(1));
        VerificationCode saved = verificationCodeRepository.save(code);
        entityManager.flush();
        entityManager.clear();

        assertThat(saved.isUsed()).isFalse();

        // When
        LocalDateTime usedAt = LocalDateTime.now();
        verificationCodeRepository.markAsUsed(saved.getId(), usedAt);
        entityManager.flush();
        entityManager.clear();

        // Then
        VerificationCode updated = verificationCodeRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.isUsed()).isTrue();
        assertThat(updated.getUsedAt()).isNotNull();
    }

    @Test
    @DisplayName("save - 应该保存验证码")
    void testSave() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        VerificationCode code = createVerificationCode(TEST_EMAIL, "123456", TYPE_PASSWORD_RESET, now.plusHours(1));

        // When
        VerificationCode saved = verificationCodeRepository.save(code);
        entityManager.flush();

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(saved.getCode()).isEqualTo("123456");
        assertThat(saved.getType()).isEqualTo(TYPE_PASSWORD_RESET);
        assertThat(saved.isUsed()).isFalse();
    }

    @Test
    @DisplayName("验证码过期检查 - isExpired() 方法")
    void testIsExpired() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        VerificationCode expiredCode = createVerificationCode(TEST_EMAIL, "111111", TYPE_PASSWORD_RESET, now.minusHours(1));
        VerificationCode validCode = createVerificationCode(TEST_EMAIL, "222222", TYPE_PASSWORD_RESET, now.plusHours(1));

        verificationCodeRepository.save(expiredCode);
        verificationCodeRepository.save(validCode);
        entityManager.flush();

        // Then
        assertThat(expiredCode.isExpired()).isTrue();
        assertThat(validCode.isExpired()).isFalse();
    }

    @Test
    @DisplayName("验证码有效性检查 - isValid() 方法")
    void testIsValid() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // 有效验证码
        VerificationCode validCode = createVerificationCode(TEST_EMAIL, "111111", TYPE_PASSWORD_RESET, now.plusHours(1));

        // 已使用验证码
        VerificationCode usedCode = createVerificationCode(TEST_EMAIL, "222222", TYPE_PASSWORD_RESET, now.plusHours(1));
        usedCode.setUsed(true);

        // 已过期验证码
        VerificationCode expiredCode = createVerificationCode(TEST_EMAIL, "333333", TYPE_PASSWORD_RESET, now.minusHours(1));

        verificationCodeRepository.save(validCode);
        verificationCodeRepository.save(usedCode);
        verificationCodeRepository.save(expiredCode);
        entityManager.flush();

        // Then
        assertThat(validCode.isValid()).isTrue();
        assertThat(usedCode.isValid()).isFalse();
        assertThat(expiredCode.isValid()).isFalse();
    }

    /**
     * 创建VerificationCode的辅助方法
     */
    private VerificationCode createVerificationCode(String email, String code, String type, LocalDateTime expiresAt) {
        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setEmail(email);
        verificationCode.setCode(code);
        verificationCode.setType(type);
        verificationCode.setCreatedAt(LocalDateTime.now());
        verificationCode.setExpiresAt(expiresAt);
        verificationCode.setUsed(false);
        verificationCode.setIpAddress("127.0.0.1");
        return verificationCode;
    }
}

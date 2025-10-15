package com.nushungry.service;

import com.nushungry.model.VerificationCode;
import com.nushungry.repository.VerificationCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 验证码服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationCodeService {

    private final VerificationCodeRepository verificationCodeRepository;
    private final EmailService emailService;

    @Value("${app.verification.code.expiration:300000}") // 默认5分钟
    private long codeExpirationMs;

    @Value("${app.verification.code.length:6}") // 默认6位
    private int codeLength;

    private static final String CODE_TYPE_PASSWORD_RESET = "PASSWORD_RESET";
    private static final String CODE_TYPE_EMAIL_VERIFY = "EMAIL_VERIFY";
    private static final int MAX_CODES_PER_HOUR = 5; // 每小时最多发送5次

    private final SecureRandom random = new SecureRandom();

    /**
     * 生成验证码
     */
    private String generateCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < codeLength; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    /**
     * 发送密码重置验证码
     *
     * @param email 邮箱地址
     * @param ipAddress IP地址
     * @return 是否发送成功
     */
    @Transactional
    public boolean sendPasswordResetCode(String email, String ipAddress) {
        try {
            // 检查发送频率
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            long recentCount = verificationCodeRepository.countRecentCodes(
                    email, CODE_TYPE_PASSWORD_RESET, oneHourAgo);

            if (recentCount >= MAX_CODES_PER_HOUR) {
                log.warn("邮箱 {} 发送验证码过于频繁", email);
                throw new RuntimeException("发送验证码过于频繁,请稍后再试");
            }

            // 生成验证码
            String code = generateCode();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresAt = now.plusSeconds(codeExpirationMs / 1000);

            // 保存验证码
            VerificationCode verificationCode = new VerificationCode();
            verificationCode.setEmail(email);
            verificationCode.setCode(code);
            verificationCode.setType(CODE_TYPE_PASSWORD_RESET);
            verificationCode.setCreatedAt(now);
            verificationCode.setExpiresAt(expiresAt);
            verificationCode.setUsed(false);
            verificationCode.setIpAddress(ipAddress);

            verificationCodeRepository.save(verificationCode);

            // 发送邮件
            emailService.sendPasswordResetCode(email, code);

            log.info("密码重置验证码已发送至: {}", email);
            return true;
        } catch (Exception e) {
            log.error("发送密码重置验证码失败: {}", e.getMessage());
            throw new RuntimeException("发送验证码失败: " + e.getMessage());
        }
    }

    /**
     * 检查密码重置验证码（不标记为已使用）
     *
     * @param email 邮箱地址
     * @param code 验证码
     * @return 是否验证成功
     */
    public boolean checkPasswordResetCode(String email, String code) {
        try {
            Optional<VerificationCode> optionalCode = verificationCodeRepository
                    .findLatestValidCode(email, CODE_TYPE_PASSWORD_RESET, LocalDateTime.now());

            if (optionalCode.isEmpty()) {
                log.warn("邮箱 {} 没有有效的验证码", email);
                return false;
            }

            VerificationCode verificationCode = optionalCode.get();

            if (!verificationCode.getCode().equals(code)) {
                log.warn("邮箱 {} 验证码不匹配", email);
                return false;
            }

            if (!verificationCode.isValid()) {
                log.warn("邮箱 {} 验证码已过期或已使用", email);
                return false;
            }

            log.info("邮箱 {} 验证码检查通过", email);
            return true;
        } catch (Exception e) {
            log.error("检查密码重置验证码失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证密码重置验证码并标记为已使用
     *
     * @param email 邮箱地址
     * @param code 验证码
     * @return 是否验证成功
     */
    @Transactional
    public boolean verifyPasswordResetCode(String email, String code) {
        try {
            Optional<VerificationCode> optionalCode = verificationCodeRepository
                    .findLatestValidCode(email, CODE_TYPE_PASSWORD_RESET, LocalDateTime.now());

            if (optionalCode.isEmpty()) {
                log.warn("邮箱 {} 没有有效的验证码", email);
                return false;
            }

            VerificationCode verificationCode = optionalCode.get();

            if (!verificationCode.getCode().equals(code)) {
                log.warn("邮箱 {} 验证码不匹配", email);
                return false;
            }

            if (!verificationCode.isValid()) {
                log.warn("邮箱 {} 验证码已过期或已使用", email);
                return false;
            }

            // 标记验证码为已使用
            verificationCodeRepository.markAsUsed(verificationCode.getId(), LocalDateTime.now());

            log.info("邮箱 {} 验证码验证成功并已标记为已使用", email);
            return true;
        } catch (Exception e) {
            log.error("验证密码重置验证码失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 清理过期的验证码
     */
    @Transactional
    public void cleanupExpiredCodes() {
        try {
            verificationCodeRepository.deleteExpiredCodes(LocalDateTime.now());
            log.info("过期验证码清理完成");
        } catch (Exception e) {
            log.error("清理过期验证码失败: {}", e.getMessage());
        }
    }
}

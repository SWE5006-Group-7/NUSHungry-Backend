package com.nushungry.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 验证码实体类
 * 用于存储邮箱验证码信息
 */
@Entity
@Table(name = "verification_codes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 邮箱地址
     */
    @Column(nullable = false)
    private String email;

    /**
     * 验证码
     */
    @Column(nullable = false, length = 10)
    private String code;

    /**
     * 验证码类型 (PASSWORD_RESET, EMAIL_VERIFY)
     */
    @Column(nullable = false, length = 50)
    private String type;

    /**
     * 创建时间
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * 过期时间
     */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /**
     * 是否已使用
     */
    @Column(nullable = false)
    private boolean used = false;

    /**
     * 使用时间
     */
    private LocalDateTime usedAt;

    /**
     * IP地址
     */
    @Column(length = 50)
    private String ipAddress;

    /**
     * 检查验证码是否过期
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 检查验证码是否有效(未过期且未使用)
     */
    public boolean isValid() {
        return !used && !isExpired();
    }
}

package com.nushungry.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Refresh Token实体
 * 用于长期保持用户登录状态，可以被服务器主动撤销
 */
@Data
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 500)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "revoked")
    private boolean revoked = false; // 是否已撤销

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt; // 最后使用时间

    @Column(name = "ip_address", length = 45)
    private String ipAddress; // 创建时的IP地址

    @Column(name = "user_agent", length = 500)
    private String userAgent; // 浏览器/设备信息

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * 检查Token是否已过期
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 检查Token是否可用（未过期且未撤销）
     */
    public boolean isValid() {
        return !isExpired() && !revoked;
    }

    /**
     * 撤销Token
     */
    public void revoke() {
        this.revoked = true;
        this.revokedAt = LocalDateTime.now();
    }

    /**
     * 更新最后使用时间
     */
    public void updateLastUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }
}

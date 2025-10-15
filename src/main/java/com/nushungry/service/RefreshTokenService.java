package com.nushungry.service;

import com.nushungry.model.RefreshToken;
import com.nushungry.model.User;
import com.nushungry.repository.RefreshTokenRepository;
import com.nushungry.repository.UserRepository;
import com.nushungry.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    /**
     * 创建新的 Refresh Token
     * @param userId 用户ID
     * @param ipAddress 客户端IP地址
     * @param userAgent 客户端User-Agent
     * @return 生成的 Refresh Token 字符串
     */
    @Transactional
    public String createRefreshToken(Long userId, String ipAddress, String userAgent) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 使用 JwtUtil 生成 JWT refresh token
        String tokenString = jwtUtil.generateRefreshToken(user.getUsername());

        // 创建 RefreshToken 实体并保存到数据库
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(tokenString);
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(jwtUtil.getRefreshTokenExpiration() / 1000));
        refreshToken.setRevoked(false);
        refreshToken.setLastUsedAt(LocalDateTime.now());
        refreshToken.setIpAddress(ipAddress);
        refreshToken.setUserAgent(userAgent);

        refreshTokenRepository.save(refreshToken);

        return tokenString;
    }

    /**
     * 验证并使用 Refresh Token 获取新的 Access Token
     * @param refreshTokenString Refresh Token 字符串
     * @return 包含新 Access Token 的对象
     */
    @Transactional
    public RefreshTokenResult useRefreshToken(String refreshTokenString) {
        // 验证 JWT token 格式和签名
        if (!jwtUtil.validateToken(refreshTokenString)) {
            throw new RuntimeException("无效的 Refresh Token");
        }

        // 从数据库中查找 token
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenString)
                .orElseThrow(() -> new RuntimeException("Refresh Token 不存在"));

        // 检查 token 是否有效(未过期且未被撤销)
        if (!refreshToken.isValid()) {
            throw new RuntimeException("Refresh Token 已失效");
        }

        // 更新最后使用时间
        refreshToken.setLastUsedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);

        // 提取用户信息并生成新的 Access Token
        String username = jwtUtil.extractUsername(refreshTokenString);
        User user = refreshToken.getUser();

        // 生成新的 Access Token,包含用户 ID
        java.util.Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("userId", user.getId());
        String newAccessToken = jwtUtil.generateAccessToken(username, claims);

        return new RefreshTokenResult(newAccessToken, jwtUtil.getAccessTokenExpiration());
    }

    /**
     * 撤销用户的 Refresh Token
     * @param refreshTokenString Refresh Token 字符串
     */
    @Transactional
    public void revokeRefreshToken(String refreshTokenString) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenString)
                .orElseThrow(() -> new RuntimeException("Refresh Token 不存在"));

        refreshToken.setRevoked(true);
        refreshToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);
    }

    /**
     * 撤销用户的所有 Refresh Token(用于强制登出所有设备)
     * @param userId 用户ID
     * @return 撤销的 token 数量
     */
    @Transactional
    public int revokeAllUserTokens(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        return refreshTokenRepository.revokeAllUserTokens(user, LocalDateTime.now());
    }

    /**
     * 获取用户所有有效的 Refresh Token
     * @param userId 用户ID
     * @return Refresh Token 列表
     */
    public List<RefreshToken> getUserValidTokens(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        return refreshTokenRepository.findValidTokensByUser(user, LocalDateTime.now());
    }

    /**
     * 清理过期的 Refresh Token(定时任务使用)
     * @return 清理的 token 数量
     */
    @Transactional
    public int cleanupExpiredTokens() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        return refreshTokenRepository.deleteExpiredTokens(thirtyDaysAgo);
    }

    /**
     * Refresh Token 使用结果
     */
    public static class RefreshTokenResult {
        private final String accessToken;
        private final Long expiresIn;

        public RefreshTokenResult(String accessToken, Long expiresIn) {
            this.accessToken = accessToken;
            this.expiresIn = expiresIn;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public Long getExpiresIn() {
            return expiresIn;
        }
    }
}

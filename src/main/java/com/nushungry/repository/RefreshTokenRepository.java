package com.nushungry.repository;

import com.nushungry.model.RefreshToken;
import com.nushungry.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 根据token字符串查找
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * 查找用户的所有有效Refresh Token
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user = :user AND rt.revoked = false AND rt.expiresAt > :now")
    List<RefreshToken> findValidTokensByUser(@Param("user") User user, @Param("now") LocalDateTime now);

    /**
     * 查找用户的所有Refresh Token（包括已撤销的）
     */
    List<RefreshToken> findByUser(User user);

    /**
     * 撤销用户的所有Refresh Token
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :now WHERE rt.user = :user AND rt.revoked = false")
    int revokeAllUserTokens(@Param("user") User user, @Param("now") LocalDateTime now);

    /**
     * 删除已过期的Token（清理任务）
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :date")
    int deleteExpiredTokens(@Param("date") LocalDateTime date);

    /**
     * 删除用户的所有已撤销Token
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user = :user AND rt.revoked = true")
    int deleteRevokedTokensByUser(@Param("user") User user);

    /**
     * 统计用户的有效Token数量
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user = :user AND rt.revoked = false AND rt.expiresAt > :now")
    long countValidTokensByUser(@Param("user") User user, @Param("now") LocalDateTime now);
}

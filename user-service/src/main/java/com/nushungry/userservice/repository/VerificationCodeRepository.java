package com.nushungry.userservice.repository;

import com.nushungry.userservice.model.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 验证码数据访问层
 */
@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    /**
     * 查找指定邮箱和类型的最新有效验证码
     *
     * @param email 邮箱地址
     * @param type 验证码类型
     * @param now 当前时间
     * @return 验证码
     */
    @Query("SELECT vc FROM VerificationCode vc WHERE vc.email = :email " +
           "AND vc.type = :type AND vc.used = false AND vc.expiresAt > :now " +
           "ORDER BY vc.createdAt DESC LIMIT 1")
    Optional<VerificationCode> findLatestValidCode(String email, String type, LocalDateTime now);

    /**
     * 查找指定邮箱的所有验证码
     *
     * @param email 邮箱地址
     * @return 验证码列表
     */
    List<VerificationCode> findByEmail(String email);

    /**
     * 删除过期的验证码
     *
     * @param now 当前时间
     */
    @Modifying
    @Query("DELETE FROM VerificationCode vc WHERE vc.expiresAt < :now")
    void deleteExpiredCodes(LocalDateTime now);

    /**
     * 统计指定邮箱和类型在指定时间段内创建的验证码数量
     *
     * @param email 邮箱地址
     * @param type 验证码类型
     * @param since 起始时间
     * @return 验证码数量
     */
    @Query("SELECT COUNT(vc) FROM VerificationCode vc WHERE vc.email = :email " +
           "AND vc.type = :type AND vc.createdAt > :since")
    long countRecentCodes(String email, String type, LocalDateTime since);

    /**
     * 标记验证码为已使用
     *
     * @param id 验证码ID
     * @param usedAt 使用时间
     */
    @Modifying
    @Query("UPDATE VerificationCode vc SET vc.used = true, vc.usedAt = :usedAt WHERE vc.id = :id")
    void markAsUsed(Long id, LocalDateTime usedAt);
}

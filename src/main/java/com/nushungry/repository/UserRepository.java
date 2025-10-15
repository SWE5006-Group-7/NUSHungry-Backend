package com.nushungry.repository;

import com.nushungry.model.User;
import com.nushungry.model.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    /**
     * 根据角色统计用户数量
     */
    long countByRole(UserRole role);

    /**
     * 统计指定时间之后创建的用户数量
     */
    long countByCreatedAtAfter(LocalDateTime dateTime);

    /**
     * 统计指定时间范围内创建的用户数量
     */
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 按用户名或邮箱搜索用户（分页）
     */
    Page<User> findByUsernameContainingOrEmailContaining(String username, String email, Pageable pageable);

    /**
     * 按角色查询用户（分页）
     */
    Page<User> findByRole(UserRole role, Pageable pageable);

    /**
     * 按启用状态查询用户（分页）
     */
    Page<User> findByEnabled(boolean enabled, Pageable pageable);

    /**
     * 批量更新用户启用状态
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.enabled = :enabled WHERE u.id IN :ids")
    int updateEnabledStatusByIds(@Param("ids") List<Long> ids, @Param("enabled") boolean enabled);

    /**
     * 统计指定时间之前创建的用户数量
     */
    long countByCreatedAtBefore(LocalDateTime dateTime);

    /**
     * 统计最后登录时间在指定时间之后的用户数量（活跃用户）
     */
    long countByLastLoginAfter(LocalDateTime dateTime);
}

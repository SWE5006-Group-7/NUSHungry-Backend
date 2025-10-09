package com.nushungry.controller;

import com.nushungry.dto.ChangePasswordDTO;
import com.nushungry.model.User;
import com.nushungry.model.UserRole;
import com.nushungry.repository.UserRepository;
import com.nushungry.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理员用户管理控制器
 * 提供用户管理相关的API端点
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin User Management", description = "管理员用户管理接口")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 分页查询用户列表
     */
    @GetMapping
    @Operation(summary = "获取用户列表", description = "分页查询所有用户，支持搜索和筛选")
    public ResponseEntity<Map<String, Object>> getUserList(
            @Parameter(description = "页码（从0开始）") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "排序方向（ASC/DESC）") @RequestParam(defaultValue = "DESC") String sortDirection,
            @Parameter(description = "搜索关键词（用户名或邮箱）") @RequestParam(required = false) String search,
            @Parameter(description = "角色筛选") @RequestParam(required = false) String role,
            @Parameter(description = "状态筛选（enabled/disabled）") @RequestParam(required = false) String status) {

        try {
            // 构建排序
            Sort sort = sortDirection.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

            // 构建分页
            Pageable pageable = PageRequest.of(page, size, sort);

            // 执行查询（这里简化处理，实际项目中应该使用Specification进行动态查询）
            Page<User> usersPage;

            if (search != null && !search.trim().isEmpty()) {
                // 如果有搜索条件，按用户名或邮箱搜索
                usersPage = userRepository.findByUsernameContainingOrEmailContaining(
                    search, search, pageable);
            } else if (role != null && !role.trim().isEmpty()) {
                // 如果有角色筛选
                UserRole userRole = UserRole.valueOf(role);
                usersPage = userRepository.findByRole(userRole, pageable);
            } else if (status != null) {
                // 如果有状态筛选
                boolean enabled = "enabled".equalsIgnoreCase(status);
                usersPage = userRepository.findByEnabled(enabled, pageable);
            } else {
                // 无筛选条件，返回所有用户
                usersPage = userRepository.findAll(pageable);
            }

            // 转换为安全的DTO（隐藏密码等敏感信息）
            List<Map<String, Object>> users = usersPage.getContent().stream()
                .map(this::userToDTO)
                .collect(Collectors.toList());

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("users", users);
            response.put("currentPage", usersPage.getNumber());
            response.put("totalItems", usersPage.getTotalElements());
            response.put("totalPages", usersPage.getTotalPages());
            response.put("pageSize", usersPage.getSize());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching user list", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "获取用户列表失败"));
        }
    }

    /**
     * 获取单个用户详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取用户详情", description = "根据用户ID获取详细信息")
    public ResponseEntity<?> getUserDetail(@PathVariable Long id) {
        try {
            Optional<User> userOpt = userRepository.findById(id);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "用户不存在"));
            }

            return ResponseEntity.ok(userToDetailDTO(userOpt.get()));

        } catch (Exception e) {
            log.error("Error fetching user detail for id: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "获取用户详情失败"));
        }
    }

    /**
     * 冻结/解冻账户
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "更新用户状态", description = "冻结或解冻用户账户")
    public ResponseEntity<?> updateUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request) {

        try {
            Boolean enabled = request.get("enabled");
            if (enabled == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "缺少enabled参数"));
            }

            Optional<User> userOpt = userRepository.findById(id);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "用户不存在"));
            }

            User user = userOpt.get();

            // 防止禁用自己的账户
            User currentUser = userService.getCurrentUser();
            if (currentUser.getId().equals(id) && !enabled) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "不能禁用自己的账户"));
            }

            // 更新状态
            user.setEnabled(enabled);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            log.info("User status updated - ID: {}, Enabled: {} by admin: {}",
                id, enabled, currentUser.getUsername());

            return ResponseEntity.ok(Map.of(
                "message", enabled ? "账户已启用" : "账户已禁用",
                "user", userToDTO(user)
            ));

        } catch (Exception e) {
            log.error("Error updating user status for id: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "更新用户状态失败"));
        }
    }

    /**
     * 修改用户角色
     */
    @PutMapping("/{id}/role")
    @Operation(summary = "修改用户角色", description = "更新用户的角色权限")
    public ResponseEntity<?> updateUserRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        try {
            String roleStr = request.get("role");
            if (roleStr == null || roleStr.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "缺少role参数"));
            }

            UserRole newRole;
            try {
                newRole = UserRole.valueOf(roleStr);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "无效的角色: " + roleStr));
            }

            Optional<User> userOpt = userRepository.findById(id);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "用户不存在"));
            }

            User user = userOpt.get();

            // 防止修改自己的角色
            User currentUser = userService.getCurrentUser();
            if (currentUser.getId().equals(id)) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "不能修改自己的角色"));
            }

            // 更新角色
            UserRole oldRole = user.getRole();
            user.setRole(newRole);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            log.info("User role updated - ID: {}, Role: {} -> {} by admin: {}",
                id, oldRole, newRole, currentUser.getUsername());

            return ResponseEntity.ok(Map.of(
                "message", "角色修改成功",
                "user", userToDTO(user)
            ));

        } catch (Exception e) {
            log.error("Error updating user role for id: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "修改用户角色失败"));
        }
    }

    /**
     * 重置用户密码
     */
    @PostMapping("/{id}/reset-password")
    @Operation(summary = "重置密码", description = "为用户重置密码")
    public ResponseEntity<?> resetUserPassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        try {
            String newPassword = request.get("password");
            if (newPassword == null || newPassword.length() < 6) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "密码长度至少6位"));
            }

            Optional<User> userOpt = userRepository.findById(id);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "用户不存在"));
            }

            User user = userOpt.get();

            // 加密新密码
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            // 记录操作日志
            User currentUser = userService.getCurrentUser();
            log.info("Password reset for user - ID: {}, Username: {} by admin: {}",
                id, user.getUsername(), currentUser.getUsername());

            // 发送邮件通知用户（TODO: 实现邮件服务）

            return ResponseEntity.ok(Map.of(
                "message", "密码重置成功",
                "username", user.getUsername()
            ));

        } catch (Exception e) {
            log.error("Error resetting password for user id: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "重置密码失败"));
        }
    }

    /**
     * 批量操作用户
     */
    @PostMapping("/batch")
    @Operation(summary = "批量操作", description = "批量启用、禁用或删除用户")
    public ResponseEntity<?> batchOperation(
            @RequestBody Map<String, Object> request) {

        try {
            List<Long> userIds = (List<Long>) request.get("userIds");
            String operation = (String) request.get("operation");

            if (userIds == null || userIds.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "请选择要操作的用户"));
            }

            if (operation == null || operation.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "请指定操作类型"));
            }

            User currentUser = userService.getCurrentUser();

            // 确保不能操作自己
            if (userIds.contains(currentUser.getId())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "不能对自己的账户进行批量操作"));
            }

            int affectedCount = 0;

            switch (operation.toLowerCase()) {
                case "enable":
                    affectedCount = userRepository.updateEnabledStatusByIds(userIds, true);
                    break;
                case "disable":
                    affectedCount = userRepository.updateEnabledStatusByIds(userIds, false);
                    break;
                case "delete":
                    // 软删除或硬删除，根据业务需求决定
                    userRepository.deleteAllById(userIds);
                    affectedCount = userIds.size();
                    break;
                default:
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "不支持的操作类型: " + operation));
            }

            log.info("Batch operation performed - Operation: {}, Affected: {}, By: {}",
                operation, affectedCount, currentUser.getUsername());

            return ResponseEntity.ok(Map.of(
                "message", String.format("批量操作成功，影响 %d 个用户", affectedCount),
                "affectedCount", affectedCount
            ));

        } catch (Exception e) {
            log.error("Error performing batch operation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "批量操作失败"));
        }
    }

    /**
     * 获取最新注册用户
     */
    @GetMapping("/latest")
    @Operation(summary = "获取最新用户", description = "获取最近注册的用户列表")
    public ResponseEntity<List<Map<String, Object>>> getLatestUsers(
            @Parameter(description = "限制数量") @RequestParam(defaultValue = "10") int limit) {

        try {
            Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
            Page<User> latestUsers = userRepository.findAll(pageable);

            List<Map<String, Object>> userDTOs = latestUsers.getContent().stream()
                .map(this::userToDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok(userDTOs);

        } catch (Exception e) {
            log.error("Error fetching latest users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ArrayList<>());
        }
    }

    /**
     * 管理员修改自己的密码
     */
    @PostMapping("/change-password")
    @Operation(summary = "修改密码", description = "管理员修改自己的密码")
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody ChangePasswordDTO changePasswordDTO) {

        try {
            // 获取当前登录的管理员用户
            User currentUser = userService.getCurrentUser();

            // 验证新密码和确认密码是否匹配
            if (!changePasswordDTO.getNewPassword().equals(changePasswordDTO.getConfirmPassword())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "新密码和确认密码不匹配"));
            }

            // 验证当前密码是否正确
            if (!passwordEncoder.matches(changePasswordDTO.getCurrentPassword(), currentUser.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "当前密码不正确"));
            }

            // 验证新密码不能与当前密码相同
            if (changePasswordDTO.getCurrentPassword().equals(changePasswordDTO.getNewPassword())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "新密码不能与当前密码相同"));
            }

            // 更新密码
            currentUser.setPassword(passwordEncoder.encode(changePasswordDTO.getNewPassword()));
            currentUser.setUpdatedAt(LocalDateTime.now());
            userRepository.save(currentUser);

            log.info("Admin user {} changed password successfully", currentUser.getUsername());

            return ResponseEntity.ok(Map.of(
                "message", "密码修改成功",
                "username", currentUser.getUsername()
            ));

        } catch (Exception e) {
            log.error("Error changing password", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "密码修改失败"));
        }
    }

    /**
     * 将User实体转换为安全的DTO
     */
    private Map<String, Object> userToDTO(User user) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", user.getId());
        dto.put("username", user.getUsername());
        dto.put("email", user.getEmail());
        dto.put("role", user.getRole().getValue());
        dto.put("enabled", user.isEnabled());
        dto.put("avatarUrl", user.getAvatarUrl());
        dto.put("createdAt", user.getCreatedAt());
        dto.put("updatedAt", user.getUpdatedAt());
        return dto;
    }

    /**
     * 将User实体转换为详细的DTO
     */
    private Map<String, Object> userToDetailDTO(User user) {
        Map<String, Object> dto = userToDTO(user);
        // 添加更多详细信息
        dto.put("isAdmin", user.isAdmin());
        dto.put("accountNonExpired", user.isAccountNonExpired());
        dto.put("accountNonLocked", user.isAccountNonLocked());
        dto.put("credentialsNonExpired", user.isCredentialsNonExpired());
        return dto;
    }
}
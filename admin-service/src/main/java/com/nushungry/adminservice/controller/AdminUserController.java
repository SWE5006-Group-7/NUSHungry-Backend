package com.nushungry.adminservice.controller;

import com.nushungry.adminservice.dto.*;
import com.nushungry.adminservice.service.UserServiceClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin User Management", description = "管理员用户管理接口")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminUserController {

    private final UserServiceClient userServiceClient;

    /**
     * 分页查询用户列表
     */
    @GetMapping
    @Operation(summary = "获取用户列表", description = "分页查询所有用户，支持搜索和筛选")
    public ResponseEntity<?> getUserList(
            @Parameter(description = "页码（从0开始）") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "排序方向（ASC/DESC）") @RequestParam(defaultValue = "DESC") String sortDirection,
            @Parameter(description = "搜索关键词（用户名或邮箱）") @RequestParam(required = false) String search) {

        try {
            // 构建排序
            Sort sort = sortDirection.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

            // 构建分页
            Pageable pageable = PageRequest.of(page, size, sort);

            // 调用用户服务获取用户列表
            UserListResponse response = userServiceClient.getUserList(pageable, search);

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
            UserDTO user = userServiceClient.getUserById(id);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("Error fetching user detail for id: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "获取用户详情失败"));
        }
    }

    /**
     * 创建新用户
     */
    @PostMapping
    @Operation(summary = "创建用户", description = "管理员创建新用户")
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request) {
        try {
            UserDTO user = userServiceClient.createUser(request);
            return ResponseEntity.ok(Map.of("message", "用户创建成功", "user", user));
        } catch (Exception e) {
            log.error("Error creating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "创建用户失败"));
        }
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新用户", description = "更新用户的基本信息、角色和状态")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        try {
            UserDTO user = userServiceClient.updateUser(id, request);
            return ResponseEntity.ok(Map.of("message", "用户更新成功", "user", user));
        } catch (Exception e) {
            log.error("Error updating user with id: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "更新用户失败"));
        }
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "删除指定用户")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userServiceClient.deleteUser(id);
            return ResponseEntity.ok(Map.of("message", "用户删除成功"));
        } catch (Exception e) {
            log.error("Error deleting user with id: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "删除用户失败"));
        }
    }

    /**
     * 更新用户状态
     */
    @PatchMapping("/{id}/status")
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

            UserDTO user = userServiceClient.updateUserStatus(id, enabled);
            return ResponseEntity.ok(Map.of(
                "message", enabled ? "账户已启用" : "账户已禁用",
                "user", user
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

            UserDTO user = userServiceClient.updateUserRole(id, roleStr);
            return ResponseEntity.ok(Map.of(
                "message", "角色修改成功",
                "user", user
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
            @RequestBody ChangePasswordRequest request) {

        try {
            userServiceClient.resetUserPassword(id, request.getPassword());
            return ResponseEntity.ok(Map.of("message", "密码重置成功"));

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
            @RequestBody BatchOperationRequest request) {

        try {
            int affectedCount = userServiceClient.batchOperation(request);
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
}
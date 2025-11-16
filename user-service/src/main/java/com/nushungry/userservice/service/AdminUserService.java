package com.nushungry.userservice.service;

import com.nushungry.userservice.dto.*;
import com.nushungry.userservice.model.User;
import com.nushungry.userservice.model.UserRole;
import com.nushungry.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 分页查询用户列表
     */
    public UserListResponse getUserList(int page, int size, String sortBy, String sortDirection, String search) {
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<User> userPage;
        if (search != null && !search.trim().isEmpty()) {
            // 如果有搜索关键词，需要创建搜索查询
            // 这里简化实现，实际应该使用Specification或自定义查询
            userPage = userRepository.findAll(pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }

        List<UserDTO> userDTOs = userPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return UserListResponse.builder()
                .users(userDTOs)
                .currentPage(userPage.getNumber())
                .totalItems(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .pageSize(userPage.getSize())
                .build();
    }

    /**
     * 根据ID获取用户详情
     */
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + id));
        return convertToDTO(user);
    }

    /**
     * 创建新用户
     */
    @Transactional
    public UserDTO createUser(CreateUserRequest request) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在: " + request.getUsername());
        }

        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("邮箱已存在: " + request.getEmail());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(request.isEnabled());

        // 设置角色
        if (request.getRole() != null && !request.getRole().trim().isEmpty()) {
            try {
                UserRole role = UserRole.valueOf(request.getRole().toUpperCase());
                user.setRole(role);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("无效的角色: " + request.getRole());
            }
        }

        User savedUser = userRepository.save(user);
        log.info("Created new user: {}", savedUser.getUsername());

        return convertToDTO(savedUser);
    }

    /**
     * 更新用户信息
     */
    @Transactional
    public UserDTO updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + id));

        // 更新用户名（如果提供且不同）
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new RuntimeException("用户名已存在: " + request.getUsername());
            }
            user.setUsername(request.getUsername());
        }

        // 更新邮箱（如果提供且不同）
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("邮箱已存在: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }

        // 更新角色
        if (request.getRole() != null && !request.getRole().trim().isEmpty()) {
            try {
                UserRole role = UserRole.valueOf(request.getRole().toUpperCase());
                user.setRole(role);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("无效的角色: " + request.getRole());
            }
        }

        // 更新状态
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        User updatedUser = userRepository.save(user);
        log.info("Updated user: {}", updatedUser.getUsername());

        return convertToDTO(updatedUser);
    }

    /**
     * 删除用户
     */
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + id));

        userRepository.delete(user);
        log.info("Deleted user: {}", user.getUsername());
    }

    /**
     * 更新用户状态（启用/禁用）
     */
    @Transactional
    public UserDTO updateUserStatus(Long id, Boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + id));

        user.setEnabled(enabled);
        User updatedUser = userRepository.save(user);

        log.info("Updated user status: {} -> {}", user.getUsername(), enabled ? "enabled" : "disabled");

        return convertToDTO(updatedUser);
    }

    /**
     * 修改用户角色
     */
    @Transactional
    public UserDTO updateUserRole(Long id, String roleStr) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + id));

        try {
            UserRole role = UserRole.valueOf(roleStr.toUpperCase());
            user.setRole(role);
            User updatedUser = userRepository.save(user);

            log.info("Updated user role: {} -> {}", user.getUsername(), roleStr);

            return convertToDTO(updatedUser);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("无效的角色: " + roleStr);
        }
    }

    /**
     * 重置用户密码
     */
    @Transactional
    public void resetUserPassword(Long id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + id));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Reset password for user: {}", user.getUsername());
    }

    /**
     * 批量操作用户
     */
    @Transactional
    public int batchOperation(BatchOperationRequest request) {
        List<Long> userIds = request.getUserIds();
        String operation = request.getOperation();

        if (userIds == null || userIds.isEmpty()) {
            throw new RuntimeException("用户ID列表不能为空");
        }

        int affectedCount = 0;

        switch (operation.toLowerCase()) {
            case "enable":
                for (Long userId : userIds) {
                    try {
                        updateUserStatus(userId, true);
                        affectedCount++;
                    } catch (Exception e) {
                        log.error("Failed to enable user: " + userId, e);
                    }
                }
                break;

            case "disable":
                for (Long userId : userIds) {
                    try {
                        updateUserStatus(userId, false);
                        affectedCount++;
                    } catch (Exception e) {
                        log.error("Failed to disable user: " + userId, e);
                    }
                }
                break;

            case "delete":
                for (Long userId : userIds) {
                    try {
                        deleteUser(userId);
                        affectedCount++;
                    } catch (Exception e) {
                        log.error("Failed to delete user: " + userId, e);
                    }
                }
                break;

            default:
                throw new RuntimeException("无效的操作类型: " + operation);
        }

        log.info("Batch operation {} completed, affected {} users", operation, affectedCount);

        return affectedCount;
    }

    /**
     * 获取最新注册用户列表
     */
    public List<UserDTO> getLatestUsers(int limit) {
        Pageable pageable = PageRequest.of(0, limit,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return userRepository.findAll(pageable).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 管理员修改用户密码
     */
    @Transactional
    public void changeUserPassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + userId));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Admin changed password for user: {}", user.getUsername());
    }

    /**
     * 将 User 实体转换为 UserDTO
     */
    private UserDTO convertToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().name())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }
}

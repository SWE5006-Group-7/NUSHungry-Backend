package com.nushungry.userservice.config;

import com.nushungry.userservice.model.User;
import com.nushungry.userservice.model.UserRole;
import com.nushungry.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 确保 admin 用户拥有正确的 ROLE_ADMIN 角色
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminUserInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(String... args) {
        try {
            // 查找 username 为 admin 的用户
            User adminUser = userRepository.findByUsername("admin").orElse(null);
            
            if (adminUser != null) {
                // 检查 role 是否为 ROLE_ADMIN
                if (adminUser.getRole() != UserRole.ROLE_ADMIN) {
                    log.warn("Admin user found with incorrect role: {}, updating to ROLE_ADMIN", 
                            adminUser.getRole());
                    
                    adminUser.setRole(UserRole.ROLE_ADMIN);
                    userRepository.save(adminUser);
                    
                    log.info("✅ Admin user role updated to ROLE_ADMIN successfully!");
                } else {
                    log.info("✅ Admin user already has ROLE_ADMIN role");
                }
            } else {
                log.info("ℹ️  Admin user not found - will be created on first registration");
            }
        } catch (Exception e) {
            log.error("Error initializing admin user role", e);
        }
    }
}

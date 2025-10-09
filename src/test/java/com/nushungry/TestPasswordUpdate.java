package com.nushungry;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 独立工具类: 用于生成和测试 BCrypt 密码哈希
 * 不使用 @SpringBootApplication,避免与测试框架冲突
 */
public class TestPasswordUpdate {
    public static void main(String[] args) {
        // 直接创建 BCryptPasswordEncoder,不需要 Spring 容器
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String password = "admin123";
        String encodedPassword = encoder.encode(password);

        System.out.println("=================================");
        System.out.println("Password: " + password);
        System.out.println("Encoded: " + encodedPassword);
        System.out.println("=================================");
        System.out.println("SQL to update admin password:");
        System.out.println("UPDATE users SET password='" + encodedPassword + "' WHERE username='admin';");
        System.out.println("=================================");

        // Test existing hash
        String existingHash = "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG";
        System.out.println("Does 'admin123' match existing hash? " + encoder.matches("admin123", existingHash));
        System.out.println("Does 'password' match existing hash? " + encoder.matches("password", existingHash));
    }
}

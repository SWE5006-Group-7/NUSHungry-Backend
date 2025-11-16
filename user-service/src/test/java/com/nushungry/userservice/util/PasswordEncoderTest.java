package com.nushungry.userservice.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PasswordEncoder 单元测试
 * 纯单元测试,无需 Spring 容器
 */
@DisplayName("PasswordEncoder 单元测试")
class PasswordEncoderTest {

    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
    }

    // ==================== BCrypt 加密测试 ====================

    @Test
    @DisplayName("BCrypt 加密 - 密码成功加密")
    void testEncode_Success() {
        String rawPassword = "Test@123";

        String encodedPassword = passwordEncoder.encode(rawPassword);

        // 验证加密后的密码
        assertNotNull(encodedPassword, "加密后的密码不应为null");
        assertNotEquals(rawPassword, encodedPassword, "加密后的密码应与原始密码不同");
        assertTrue(encodedPassword.startsWith("$2a$") || encodedPassword.startsWith("$2b$"),
                "BCrypt密码应以$2a$或$2b$开头");
        assertTrue(encodedPassword.length() >= 60, "BCrypt密码长度应至少为60字符");
    }

    @Test
    @DisplayName("BCrypt 加密 - 空密码处理")
    void testEncode_EmptyPassword() {
        String emptyPassword = "";

        String encodedPassword = passwordEncoder.encode(emptyPassword);

        // 即使是空字符串,BCrypt也应该能够加密
        assertNotNull(encodedPassword);
        assertTrue(encodedPassword.startsWith("$2a$") || encodedPassword.startsWith("$2b$"));
    }

    @Test
    @DisplayName("BCrypt 加密 - 特殊字符密码")
    void testEncode_SpecialCharacters() {
        String specialPassword = "!@#$%^&*()_+-=[]{}|;:',.<>?/~`";

        String encodedPassword = passwordEncoder.encode(specialPassword);

        assertNotNull(encodedPassword);
        assertTrue(passwordEncoder.matches(specialPassword, encodedPassword));
    }

    @Test
    @DisplayName("BCrypt 加密 - 长密码处理")
    void testEncode_LongPassword() {
        String longPassword = "a".repeat(100); // 100个字符的密码

        String encodedPassword = passwordEncoder.encode(longPassword);

        assertNotNull(encodedPassword);
        assertTrue(passwordEncoder.matches(longPassword, encodedPassword));
    }

    // ==================== 密码匹配验证 ====================

    @Test
    @DisplayName("密码匹配 - 正确密码验证成功")
    void testMatches_CorrectPassword() {
        String rawPassword = "Test@123";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);

        assertTrue(matches, "正确密码应匹配成功");
    }

    @Test
    @DisplayName("密码匹配 - 错误密码验证失败")
    void testMatches_WrongPassword() {
        String rawPassword = "Test@123";
        String wrongPassword = "Wrong@123";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        boolean matches = passwordEncoder.matches(wrongPassword, encodedPassword);

        assertFalse(matches, "错误密码应匹配失败");
    }

    @Test
    @DisplayName("密码匹配 - 大小写敏感")
    void testMatches_CaseSensitive() {
        String rawPassword = "Test@123";
        String differentCasePassword = "test@123";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        boolean matches = passwordEncoder.matches(differentCasePassword, encodedPassword);

        assertFalse(matches, "大小写不同的密码应匹配失败");
    }

    @Test
    @DisplayName("密码匹配 - 空密码匹配")
    void testMatches_EmptyPassword() {
        String emptyPassword = "";
        String encodedPassword = passwordEncoder.encode(emptyPassword);

        boolean matches = passwordEncoder.matches(emptyPassword, encodedPassword);

        assertTrue(matches, "空密码应能正确匹配");
    }

    @Test
    @DisplayName("密码匹配 - null密码抛出异常")
    void testMatches_NullPassword() {
        String encodedPassword = passwordEncoder.encode("Test@123");

        // BCrypt的matches方法不接受null密码,应抛出IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            passwordEncoder.matches(null, encodedPassword);
        }, "null密码应抛出IllegalArgumentException");
    }

    // ==================== 盐值随机性测试 ====================

    @Test
    @DisplayName("盐值随机性 - 相同密码生成不同哈希")
    void testSaltRandomness_SamePasswordDifferentHashes() {
        String rawPassword = "Test@123";

        String encodedPassword1 = passwordEncoder.encode(rawPassword);
        String encodedPassword2 = passwordEncoder.encode(rawPassword);

        // 验证两次加密结果不同(因为盐值不同)
        assertNotEquals(encodedPassword1, encodedPassword2,
                "相同密码多次加密应产生不同的哈希值(盐值随机)");

        // 但两个哈希都应该能匹配原始密码
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword1));
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword2));
    }

    @Test
    @DisplayName("盐值随机性 - 批量验证随机性")
    void testSaltRandomness_MultipleEncryptions() {
        String rawPassword = "Test@123";
        int iterations = 10;

        // 生成10个相同密码的哈希
        String[] hashes = new String[iterations];
        for (int i = 0; i < iterations; i++) {
            hashes[i] = passwordEncoder.encode(rawPassword);
        }

        // 验证所有哈希都不相同
        for (int i = 0; i < iterations; i++) {
            for (int j = i + 1; j < iterations; j++) {
                assertNotEquals(hashes[i], hashes[j],
                        String.format("第%d个和第%d个哈希不应相同", i + 1, j + 1));
            }
        }

        // 验证所有哈希都能匹配原始密码
        for (String hash : hashes) {
            assertTrue(passwordEncoder.matches(rawPassword, hash));
        }
    }

    // ==================== BCrypt 格式验证 ====================

    @Test
    @DisplayName("BCrypt格式 - 验证哈希格式结构")
    void testBCryptFormat_ValidStructure() {
        String rawPassword = "Test@123";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // BCrypt格式: $2a$rounds$saltsaltsaltsaltsalthashhashhashhashhashhashhash
        // 总共60个字符
        String[] parts = encodedPassword.split("\\$");

        // 分割后应有4部分: ["", "2a", "10", "salt+hash"]
        assertEquals(4, parts.length, "BCrypt哈希应有4个部分");
        assertEquals("", parts[0], "第一部分应为空字符串");
        assertTrue(parts[1].equals("2a") || parts[1].equals("2b"), "版本应为2a或2b");
        assertTrue(Integer.parseInt(parts[2]) >= 4 && Integer.parseInt(parts[2]) <= 31,
                "工作因子应在4-31之间");
        assertEquals(53, parts[3].length(), "盐值+哈希部分应为53字符");
    }

    @Test
    @DisplayName("BCrypt格式 - 验证默认工作因子")
    void testBCryptFormat_DefaultWorkFactor() {
        String rawPassword = "Test@123";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // 提取工作因子(rounds)
        String[] parts = encodedPassword.split("\\$");
        int workFactor = Integer.parseInt(parts[2]);

        // BCryptPasswordEncoder默认工作因子为10
        assertEquals(10, workFactor, "默认工作因子应为10");
    }

    // ==================== 边界情况测试 ====================

    @Test
    @DisplayName("边界情况 - 最短密码(1字符)")
    void testEdgeCase_SingleCharacterPassword() {
        String singleChar = "a";

        String encodedPassword = passwordEncoder.encode(singleChar);

        assertNotNull(encodedPassword);
        assertTrue(passwordEncoder.matches(singleChar, encodedPassword));
        assertFalse(passwordEncoder.matches("b", encodedPassword));
    }

    @Test
    @DisplayName("边界情况 - Unicode字符密码")
    void testEdgeCase_UnicodePassword() {
        String unicodePassword = "测试密码🔒";

        String encodedPassword = passwordEncoder.encode(unicodePassword);

        assertNotNull(encodedPassword);
        assertTrue(passwordEncoder.matches(unicodePassword, encodedPassword));
    }

    @Test
    @DisplayName("边界情况 - 空格密码")
    void testEdgeCase_WhitespacePassword() {
        String whitespacePassword = "   ";

        String encodedPassword = passwordEncoder.encode(whitespacePassword);

        assertNotNull(encodedPassword);
        assertTrue(passwordEncoder.matches(whitespacePassword, encodedPassword));
        assertFalse(passwordEncoder.matches("", encodedPassword), "纯空格与空字符串应不匹配");
    }

    // ==================== 性能测试 ====================

    @Test
    @DisplayName("性能验证 - 加密操作耗时合理")
    void testPerformance_EncodingTime() {
        String rawPassword = "Test@123";

        long startTime = System.currentTimeMillis();
        passwordEncoder.encode(rawPassword);
        long endTime = System.currentTimeMillis();

        long duration = endTime - startTime;

        // BCrypt是故意设计得慢的,但在测试环境应在1秒内完成
        assertTrue(duration < 1000,
                String.format("单次加密应在1秒内完成,实际耗时: %dms", duration));
    }

    @Test
    @DisplayName("性能验证 - 验证操作耗时合理")
    void testPerformance_MatchingTime() {
        String rawPassword = "Test@123";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        long startTime = System.currentTimeMillis();
        passwordEncoder.matches(rawPassword, encodedPassword);
        long endTime = System.currentTimeMillis();

        long duration = endTime - startTime;

        assertTrue(duration < 1000,
                String.format("单次验证应在1秒内完成,实际耗时: %dms", duration));
    }
}

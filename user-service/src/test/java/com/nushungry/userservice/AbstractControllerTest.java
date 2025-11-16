package com.nushungry.userservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nushungry.userservice.client.CafeteriaServiceClient;
import com.nushungry.userservice.client.ReviewServiceClient;
import com.nushungry.userservice.model.User;
import com.nushungry.userservice.model.UserRole;
import com.nushungry.userservice.repository.RefreshTokenRepository;
import com.nushungry.userservice.repository.UserRepository;
import com.nushungry.userservice.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

/**
 * 抽象Controller测试基类
 *
 * <p>提供通用的测试基础设施:</p>
 * <ul>
 *   <li>H2 内存数据库配置(PostgreSQL兼容模式)</li>
 *   <li>JWT Token 生成工具方法</li>
 *   <li>用户创建辅助方法</li>
 *   <li>Mock外部依赖(Feign Clients)</li>
 * </ul>
 *
 * <p>使用方式:</p>
 * <pre>
 * {@code
 * class MyControllerTest extends AbstractControllerTest {
 *
 *     @Test
 *     void testSomething() {
 *         // 创建测试用户
 *         User user = createTestUser("testuser", "test@example.com", UserRole.ROLE_USER);
 *
 *         // 生成JWT token
 *         String token = generateToken(user);
 *
 *         // 执行测试
 *         mockMvc.perform(get("/api/user/profile")
 *                 .header("Authorization", "Bearer " + token))
 *             .andExpect(status().isOk());
 *     }
 * }
 * }
 * </pre>
 *
 * @author NUSHungry Team
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected RefreshTokenRepository refreshTokenRepository;

    @Autowired
    protected JwtUtil jwtUtil;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    // Mock 外部依赖 - Feign Clients
    @MockBean
    protected CafeteriaServiceClient cafeteriaServiceClient;

    @MockBean
    protected ReviewServiceClient reviewServiceClient;

    /**
     * 配置H2内存数据库和测试环境
     *
     * <p>数据库配置:</p>
     * <ul>
     *   <li>PostgreSQL兼容模式: MODE=PostgreSQL</li>
     *   <li>连接保持: DB_CLOSE_DELAY=-1</li>
     *   <li>JPA策略: create-drop(每个测试自动创建删除表)</li>
     * </ul>
     */
    @DynamicPropertySource
    static void configureTestProperties(DynamicPropertyRegistry registry) {
        // H2 数据库配置 (PostgreSQL 兼容模式)
        registry.add("spring.datasource.url",
            () -> "jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");

        // JPA 配置
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.H2Dialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "false");

        // JWT 配置
        registry.add("jwt.secret",
            () -> "myTestSecretKeyForNUSHungryUserServiceThatIsLongEnoughForHS256Algorithm");
        registry.add("jwt.access-token.expiration", () -> "3600000");  // 1 hour
        registry.add("jwt.refresh-token.expiration", () -> "2592000000");  // 30 days
        registry.add("jwt.expiration", () -> "3600000");

        // 排除不需要的自动配置
        registry.add("spring.autoconfigure.exclude",
            () -> "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration," +
                  "org.springframework.cloud.openfeign.FeignAutoConfiguration," +
                  "org.springframework.cloud.openfeign.hateoas.FeignHalAutoConfiguration");
    }

    /**
     * 生成JWT Access Token
     *
     * @param user 用户对象
     * @return JWT token字符串
     */
    protected String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().name());  // 使用完整枚举名 "ROLE_USER"
        return jwtUtil.generateAccessToken(user.getUsername(), claims);
    }

    /**
     * 生成JWT Access Token (指定用户名和角色)
     *
     * @param username 用户名
     * @param userId 用户ID
     * @param role 用户角色
     * @return JWT token字符串
     */
    protected String generateToken(String username, Long userId, UserRole role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role.name());
        return jwtUtil.generateAccessToken(username, claims);
    }

    /**
     * 创建测试用户并保存到数据库
     *
     * @param username 用户名
     * @param email 邮箱
     * @param role 用户角色
     * @return 保存后的用户对象
     */
    protected User createTestUser(String username, String email, UserRole role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("Test@123"));
        user.setRole(role);
        return userRepository.save(user);
    }

    /**
     * 创建测试用户(默认角色为USER)
     *
     * @param username 用户名
     * @param email 邮箱
     * @return 保存后的用户对象
     */
    protected User createTestUser(String username, String email) {
        return createTestUser(username, email, UserRole.ROLE_USER);
    }

    /**
     * 创建管理员用户
     *
     * @param username 用户名
     * @param email 邮箱
     * @return 保存后的管理员用户对象
     */
    protected User createAdminUser(String username, String email) {
        return createTestUser(username, email, UserRole.ROLE_ADMIN);
    }

    /**
     * 清理数据库
     *
     * <p>建议在 @BeforeEach 中调用此方法确保测试隔离</p>
     */
    protected void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }
}

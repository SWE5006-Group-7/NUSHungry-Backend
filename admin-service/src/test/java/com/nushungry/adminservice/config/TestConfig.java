package com.nushungry.adminservice.config;

import com.nushungry.adminservice.filter.JwtAuthenticationFilter;
import com.nushungry.adminservice.service.UserServiceClient;
import com.nushungry.adminservice.util.JwtUtil;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

/**
 * 测试配置类
 * 提供测试所需的 Mock beans
 *
 * 注意: 使用 @MockBean 提供 Mock beans 用于 @WebMvcTest
 * 测试中使用 @WithMockUser 来模拟认证用户
 */
@TestConfiguration
public class TestConfig {

    /**
     * 提供 Mock 的 JwtUtil (用于 @WebMvcTest)
     */
    @MockBean
    private JwtUtil jwtUtil;

    /**
     * 提供 Mock 的 JwtAuthenticationFilter (用于 @WebMvcTest)
     */
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 提供 Mock 的 UserServiceClient
     * 使用 @Primary 确保在测试环境中优先使用此 bean
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.cloud.openfeign.enabled", havingValue = "false", matchIfMissing = true)
    public UserServiceClient userServiceClient() {
        return mock(UserServiceClient.class);
    }

    /**
     * 提供 Mock 的 RabbitTemplate
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.rabbitmq.enabled", havingValue = "false", matchIfMissing = true)
    public RabbitTemplate rabbitTemplate() {
        return mock(RabbitTemplate.class);
    }
}

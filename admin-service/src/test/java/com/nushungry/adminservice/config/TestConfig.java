package com.nushungry.adminservice.config;

import com.nushungry.adminservice.filter.JwtAuthenticationFilter;
import com.nushungry.adminservice.service.UserServiceClient;
import com.nushungry.adminservice.util.JwtUtil;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import static org.mockito.Mockito.mock;

/**
 * 测试配置类
 * 提供测试所需的 Mock beans 和简化的 Security 配置
 */
@TestConfiguration
@EnableWebSecurity
public class TestConfig {

    /**
     * 提供 Mock 的 UserServiceClient
     * 使用 @Primary 确保在测试环境中优先使用此 bean
     */
    @Bean
    @Primary
    public UserServiceClient userServiceClient() {
        return mock(UserServiceClient.class);
    }

    /**
     * 提供 Mock 的 JwtUtil
     */
    @Bean
    @Primary
    public JwtUtil jwtUtil() {
        return mock(JwtUtil.class);
    }

    /**
     * 提供 Mock 的 JwtAuthenticationFilter
     */
    @Bean
    @Primary
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return mock(JwtAuthenticationFilter.class);
    }

    /**
     * 简化的测试 Security 配置
     * 允许所有请求通过，使用 @WithMockUser 来模拟认证
     */
    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/admin/auth/**").permitAll()
                        .anyRequest().authenticated()
                );
        
        return http.build();
    }
}

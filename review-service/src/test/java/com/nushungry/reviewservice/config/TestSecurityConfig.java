package com.nushungry.reviewservice.config;

import com.nushungry.reviewservice.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

/**
 * 测试环境安全配置
 * 提供 MockGatewayFilter 模拟生产环境的 Gateway 行为
 */
@TestConfiguration
@Profile("test")
public class TestSecurityConfig {

    /**
     * 模拟 Gateway 的测试过滤器
     *
     * 功能:
     * 1. 从 Authorization header 提取 JWT token
     * 2. 验证 token 有效性
     * 3. 提取用户信息并注入 X-User-Id, X-Username, X-User-Role headers
     * 4. 完全模拟生产环境 Gateway 的行为
     *
     * 这样确保:
     * - 测试环境与生产环境行为一致
     * - JwtAuthenticationFilter 在测试中也能正常工作
     * - 安全测试验证真实的认证流程
     */
    @Component
    @Slf4j
    @RequiredArgsConstructor
    @Profile("test")
    public static class MockGatewayFilter extends OncePerRequestFilter {

        private final JwtUtil jwtUtil;

        // 公开端点,无需注入认证信息
        private static final List<String> PUBLIC_PATHS = Arrays.asList(
                "/actuator",
                "/swagger-ui",
                "/v3/api-docs"
        );

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {

            String requestPath = request.getRequestURI();

            // 跳过公开端点
            if (isPublicPath(requestPath)) {
                chain.doFilter(request, response);
                return;
            }

            try {
                // 尝试从 Authorization header 提取 JWT token
                final String authorizationHeader = request.getHeader("Authorization");

                if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                    String jwt = authorizationHeader.substring(7);

                    // 验证 Token
                    if (jwtUtil.validateToken(jwt)) {
                        // 提取用户信息
                        String username = jwtUtil.extractUsername(jwt);
                        Long userId = jwtUtil.extractUserId(jwt);
                        String role = jwtUtil.extractRole(jwt);

                        if (username != null && userId != null && role != null) {
                            log.debug("MockGateway: Injecting headers for userId={}, username={}, role={}",
                                     userId, username, role);

                            // 模拟 Gateway 注入 headers
                            HttpServletRequestWrapper wrapper = new HeaderInjectingRequestWrapper(
                                request,
                                userId.toString(),
                                username,
                                role.startsWith("ROLE_") ? role : "ROLE_" + role
                            );

                            chain.doFilter(wrapper, response);
                            return;
                        }
                    }
                }

                // 没有有效的认证信息,直接放行让 Spring Security 处理
                chain.doFilter(request, response);

            } catch (Exception e) {
                log.error("MockGateway: Error processing request: {}", e.getMessage());
                chain.doFilter(request, response);
            }
        }

        private boolean isPublicPath(String path) {
            return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
        }

        /**
         * HttpServletRequest 包装器,注入 Gateway headers
         */
        private static class HeaderInjectingRequestWrapper extends HttpServletRequestWrapper {
            private final String userId;
            private final String username;
            private final String role;

            public HeaderInjectingRequestWrapper(HttpServletRequest request, String userId, String username, String role) {
                super(request);
                this.userId = userId;
                this.username = username;
                this.role = role;
            }

            @Override
            public String getHeader(String name) {
                if ("X-User-Id".equalsIgnoreCase(name)) {
                    return userId;
                } else if ("X-Username".equalsIgnoreCase(name)) {
                    return username;
                } else if ("X-User-Role".equalsIgnoreCase(name)) {
                    return role;
                }
                return super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                Set<String> headerNames = new HashSet<>();
                Enumeration<String> originalHeaders = super.getHeaderNames();
                while (originalHeaders.hasMoreElements()) {
                    headerNames.add(originalHeaders.nextElement());
                }
                // 添加自定义 headers
                headerNames.add("X-User-Id");
                headerNames.add("X-Username");
                headerNames.add("X-User-Role");
                return Collections.enumeration(headerNames);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                if ("X-User-Id".equalsIgnoreCase(name)) {
                    return Collections.enumeration(Collections.singletonList(userId));
                } else if ("X-Username".equalsIgnoreCase(name)) {
                    return Collections.enumeration(Collections.singletonList(username));
                } else if ("X-User-Role".equalsIgnoreCase(name)) {
                    return Collections.enumeration(Collections.singletonList(role));
                }
                return super.getHeaders(name);
            }
        }
    }
}

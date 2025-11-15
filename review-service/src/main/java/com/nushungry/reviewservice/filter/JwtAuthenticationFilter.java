package com.nushungry.reviewservice.filter;

import com.nushungry.reviewservice.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

/**
 * JWT 认证过滤器 - 轻量级验证
 * 从 Authorization Header 提取并验证 JWT Token
 * 将用户信息注入 Spring Security Context 和请求头
 *
 * 设计理念:
 * 1. 优先使用 Gateway 注入的 headers (X-User-Id, X-Username, X-User-Role)
 * 2. 如果没有 Gateway headers,则从 JWT Token 提取用户信息
 * 3. 测试环境通过 MockGatewayFilter 模拟 Gateway 行为,保持环境一致性
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    // 公开端点,无需认证
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
            // 优先使用网关注入的 headers (微服务架构下的标准模式)
            String userIdHeader = request.getHeader("X-User-Id");
            String usernameHeader = request.getHeader("X-Username");
            String roleHeader = request.getHeader("X-User-Role");

            // DEBUG: 输出所有请求headers
            log.info("Request path: {}, X-User-Id: {}, X-Username: {}, X-User-Role: {}",
                     requestPath, userIdHeader, usernameHeader, roleHeader);

            // 如果网关已注入用户信息,直接使用
            if (userIdHeader != null && usernameHeader != null) {
                log.info("Authentication from gateway headers: userId={}, username={}, role={}",
                         userIdHeader, usernameHeader, roleHeader);

                Long userId = Long.parseLong(userIdHeader);
                String role = roleHeader != null ? roleHeader : "USER";

                // 设置认证信息
                setAuthentication(request, userId, usernameHeader, role);

                // 直接传递,不需要包装器
                chain.doFilter(request, response);
                return;
            }

            // 否则尝试从 Authorization header 提取 JWT (用于直接调用或开发环境)
            final String authorizationHeader = request.getHeader("Authorization");

            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                log.warn("Missing authentication: no gateway headers or Authorization header for path: {}", requestPath);
                // 让 Spring Security 的 AuthenticationEntryPoint 处理
                chain.doFilter(request, response);
                return;
            }

            String jwt = authorizationHeader.substring(7);

            // 验证 Token
            if (!jwtUtil.validateToken(jwt)) {
                log.warn("Invalid or expired token for path: {}", requestPath);
                // 让 Spring Security 处理
                chain.doFilter(request, response);
                return;
            }

            // 提取用户信息
            String username = jwtUtil.extractUsername(jwt);
            Long userId = jwtUtil.extractUserId(jwt);
            String role = jwtUtil.extractRole(jwt);

            // 验证必要的 claims
            if (username == null || userId == null || role == null || role.isEmpty()) {
                log.warn("Missing required claims in token (username={}, userId={}, role={}) for path: {}",
                        username, userId, role, requestPath);
                // 让 Spring Security 处理
                chain.doFilter(request, response);
                return;
            }

            log.debug("Authentication from JWT: userId={}, username={}, role={}", userId, username, role);

            // 设置认证信息
            setAuthentication(request, userId, username, role);

            // 包装器需要注入带 ROLE_ 前缀的角色
            String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;

            // 使用包装器让控制器的 @RequestHeader 能读取到值
            HttpServletRequestWrapper wrapper = new HeaderInjectingRequestWrapper(request, userId.toString(), username, roleWithPrefix);
            chain.doFilter(wrapper, response);

        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            // 让 Spring Security 的 AuthenticationEntryPoint 处理
            chain.doFilter(request, response);
        }
    }

    /**
     * 设置认证信息到 Spring Security Context 和 Request Attributes
     */
    private void setAuthentication(HttpServletRequest request, Long userId, String username, String role) {
        // Spring Security 权限需要 ROLE_ 前缀
        String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;

        // 创建 Spring Security Authentication
        SimpleGrantedAuthority grantedAuthority = new SimpleGrantedAuthority(authority);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(username, null, Collections.singletonList(grantedAuthority));

        // 将 userId 存储到 authentication details
        authentication.setDetails(userId);

        // 设置到 Security Context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 同时添加到请求属性(兼容现有代码)
        request.setAttribute("username", username);
        request.setAttribute("userId", userId);
        request.setAttribute("role", authority); // 存储带 ROLE_ 前缀的角色

        log.debug("Set authentication: username={}, userId={}, role={}", username, userId, authority);
    }

    /**
     * 检查是否为公开路径
     */
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * HttpServletRequest 包装器,注入自定义 headers
     * 让控制器的 @RequestHeader 能读取到 JWT 解析出的用户信息
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

package com.nushungry.preference.filter;

import com.nushungry.preference.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * JWT 认证过滤器 - 轻量级验证
 * 从 Authorization Header 提取并验证 JWT Token
 * 将用户信息注入请求头供后续使用
 *
 * 注意:在测试环境中禁用,使用 @Profile("!test") 排除
 */
@Slf4j
@Component
@Profile("!test")  // 在测试环境中不加载此过滤器
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

        final String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}", requestPath);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Missing or invalid token\"}");
            return;
        }

        String jwt = authorizationHeader.substring(7);

        try {
            // 验证 Token
            if (!jwtUtil.validateToken(jwt)) {
                log.warn("Invalid or expired token for path: {}", requestPath);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Invalid or expired token\"}");
                return;
            }

            // 提取用户信息并注入请求头
            String username = jwtUtil.extractUsername(jwt);
            Long userId = jwtUtil.extractUserId(jwt);
            String role = jwtUtil.extractRole(jwt);

            // 添加用户信息到请求属性
            request.setAttribute("username", username);
            request.setAttribute("userId", userId);
            request.setAttribute("role", role);

            log.debug("Authenticated user: {} (ID: {}, Role: {}) for path: {}", username, userId, role, requestPath);

        } catch (Exception e) {
            log.error("JWT validation error: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Token validation failed\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * 检查是否为公开路径
     */
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }
}

package com.nushungry.interceptor;

import com.nushungry.model.User;
import com.nushungry.model.UserRole;
import com.nushungry.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 角色检查拦截器
 * 用于增强角色验证和访问控制
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoleCheckInterceptor implements HandlerInterceptor {

    private final UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();

        // 忽略不需要验证的路径
        if (shouldIgnore(requestURI)) {
            return true;
        }

        // 检查管理员路径
        if (requestURI.startsWith("/api/admin")) {
            return checkAdminAccess(request, response);
        }

        return true;
    }

    /**
     * 检查管理员访问权限
     */
    private boolean checkAdminAccess(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            User currentUser = userService.getCurrentUser();

            if (currentUser == null) {
                log.warn("Unauthorized access attempt to admin API - no user found");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
                return false;
            }

            if (currentUser.getRole() != UserRole.ROLE_ADMIN) {
                log.warn("Forbidden access attempt to admin API by user: {} with role: {}",
                        currentUser.getUsername(), currentUser.getRole());
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin role required");
                return false;
            }

            log.debug("Admin access granted to user: {}", currentUser.getUsername());
            return true;

        } catch (Exception e) {
            log.error("Error checking admin access", e);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
            return false;
        }
    }


    /**
     * 判断是否应该忽略该路径
     */
    private boolean shouldIgnore(String requestURI) {
        return requestURI.startsWith("/api/auth") ||
               requestURI.startsWith("/api/admin/auth/login") ||  // 管理员登录接口
               requestURI.startsWith("/api/admin/auth/refresh") || // 管理员token刷新接口
               requestURI.startsWith("/api/public") ||
               requestURI.startsWith("/v3/api-docs") ||
               requestURI.startsWith("/swagger-ui");
    }
}
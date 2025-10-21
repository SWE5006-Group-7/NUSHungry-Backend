package com.nushungry.gateway.filter;

import com.nushungry.gateway.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * Global JWT Authentication Filter
 *
 * This filter validates JWT tokens for all incoming requests except
 * for public endpoints (login, register, etc.)
 */
@Slf4j
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtUtil jwtUtil;

    // Public endpoints that don't require authentication
    private static final List<String> PUBLIC_URLS = Arrays.asList(
            "/api/admin/auth/login",
            "/api/admin/auth/register",
            "/api/cafeterias",
            "/api/stalls",
            "/media/images",
            "/actuator"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Skip authentication for public URLs
        if (isPublicUrl(path)) {
            return chain.filter(exchange);
        }

        // Extract Authorization header
        String authHeader = request.getHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        // Validate token
        if (!jwtUtil.validateToken(token) || jwtUtil.isTokenExpired(token)) {
            log.warn("Invalid or expired token for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // Extract user information and add to headers for downstream services
        String userId = jwtUtil.extractUserId(token);
        String username = jwtUtil.extractUsername(token);
        String role = jwtUtil.extractRole(token);

        // Add user information to request headers
        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-Id", userId)
                .header("X-Username", username)
                .header("X-User-Role", role)
                .build();

        log.debug("Authenticated user: {} (ID: {}, Role: {}) for path: {}",
                username, userId, role, path);

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    /**
     * Check if the URL is public (doesn't require authentication)
     */
    private boolean isPublicUrl(String path) {
        return PUBLIC_URLS.stream().anyMatch(path::startsWith);
    }

    @Override
    public int getOrder() {
        return -100; // Execute before other filters
    }
}

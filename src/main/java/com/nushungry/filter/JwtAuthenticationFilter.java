package com.nushungry.filter;

import com.nushungry.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    // 不需要认证的路径
    private static final List<String> SKIP_AUTH_PATHS = Arrays.asList(
        "/api/auth/login",
        "/api/auth/register",
        "/api/cafeterias",
        "/api/stalls",
        "/api/images",
        "/v3/api-docs",
        "/swagger-ui",
        "/swagger-ui.html"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        String method = request.getMethod();

        // 跳过不需要认证的路径
        if (shouldSkipAuthentication(requestPath)) {
            chain.doFilter(request, response);
            return;
        }

        // 评价接口：只有 GET 请求可以跳过认证
        if (requestPath.startsWith("/api/reviews") && "GET".equalsIgnoreCase(method)) {
            chain.doFilter(request, response);
            return;
        }

        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        logger.info("Processing request: " + method + " " + requestPath);
        logger.info("Authorization header: " + (authorizationHeader != null ? "Present" : "Missing"));

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(jwt);
                logger.info("Extracted username from token: " + username);
            } catch (Exception e) {
                // Invalid token
                logger.error("JWT token validation error: " + e.getMessage(), e);
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                logger.info("Loaded user details for: " + username);

                if (jwtUtil.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    logger.info("Authentication successful for user: " + username);
                } else {
                    logger.warn("Token validation failed for user: " + username);
                }
            } catch (Exception e) {
                logger.error("Error loading user details: " + e.getMessage(), e);
            }
        } else if (username == null) {
            logger.warn("No username extracted from token");
        }
        chain.doFilter(request, response);
    }

    private boolean shouldSkipAuthentication(String requestPath) {
        return SKIP_AUTH_PATHS.stream()
            .anyMatch(path -> requestPath.startsWith(path));
    }
}

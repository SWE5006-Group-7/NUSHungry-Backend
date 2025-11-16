package com.nushungry.userservice.controller;

import com.nushungry.userservice.dto.AuthResponse;
import com.nushungry.userservice.dto.LoginRequest;
import com.nushungry.userservice.dto.RefreshTokenRequest;
import com.nushungry.userservice.dto.RefreshTokenResponse;
import com.nushungry.userservice.dto.RegisterRequest;
import com.nushungry.userservice.service.RefreshTokenService;
import com.nushungry.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication endpoints")
public class AuthController {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping(value = "/register", produces = "application/json")
    @Operation(summary = "Register a new user")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            log.info("Register request from IP: {}, User-Agent: {}", ipAddress, userAgent);

            AuthResponse response = userService.register(request, ipAddress, userAgent);
            log.info("User successfully registered: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            log.error("Registration failed for user {}: {}", request.getUsername(), e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping(value = "/login", produces = "application/json")
    @Operation(summary = "Login user")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            log.info("Login request for user: {} from IP: {}", request.getUsername(), ipAddress);

            AuthResponse response = userService.login(request, ipAddress, userAgent);
            log.info("User successfully logged in: {}", request.getUsername());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Login failed for user {}: {}", request.getUsername(), e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            log.info("Token refresh request received");

            RefreshTokenService.RefreshTokenResult result =
                refreshTokenService.useRefreshToken(request.getRefreshToken());

            RefreshTokenResponse response = new RefreshTokenResponse(
                result.getAccessToken(),
                result.getExpiresIn()
            );

            log.info("Token successfully refreshed");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(401).body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user and revoke refresh token")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            log.info("Logout request received");
            refreshTokenService.revokeRefreshToken(request.getRefreshToken());
            log.info("User successfully logged out");
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            log.error("Logout failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * 错误响应DTO
     */
    private static class ErrorResponse {
        private final String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }
    }
}
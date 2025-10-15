package com.nushungry.controller;

import com.nushungry.dto.AuthResponse;
import com.nushungry.dto.ForgotPasswordRequest;
import com.nushungry.dto.LoginRequest;
import com.nushungry.dto.RefreshTokenRequest;
import com.nushungry.dto.RefreshTokenResponse;
import com.nushungry.dto.RegisterRequest;
import com.nushungry.dto.ResetPasswordRequest;
import com.nushungry.service.UserService;
import com.nushungry.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication endpoints")
public class AuthController {

    private final UserService userService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<AuthResponse> register(
            @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            AuthResponse response = userService.register(request, ipAddress, userAgent);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Login user")
    public ResponseEntity<AuthResponse> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            AuthResponse response = userService.login(request, ipAddress, userAgent);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Send verification code to email for password reset")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        if (request == null || !StringUtils.hasText(request.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Email is required."
            ));
        }
        try {
            passwordResetService.sendPasswordResetCode(request.getEmail().trim());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Verification code sent to email."
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", ex.getMessage()
            ));
        } catch (IllegalStateException ex) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", ex.getMessage()
            ));
        }
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Verify code and reset password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        if (request == null
                || !StringUtils.hasText(request.getEmail())
                || !StringUtils.hasText(request.getCode())
                || !StringUtils.hasText(request.getNewPassword())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Email, code, and new password are required."
            ));
        }

        try {
            passwordResetService.verifyCodeAndResetPassword(
                    request.getEmail().trim(),
                    request.getCode().trim(),
                    request.getNewPassword()
            );
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Password reset successfully."
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", ex.getMessage()
            ));
        }
    }
}

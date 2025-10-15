package com.nushungry.controller;

import com.nushungry.dto.ForgotPasswordRequest;
import com.nushungry.dto.ResetPasswordRequest;
import com.nushungry.dto.VerifyResetCodeRequest;
import com.nushungry.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/password")
@RequiredArgsConstructor
@Tag(name = "Password Reset", description = "Forgot password and verification endpoints")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/send-reset-code")
    @Operation(summary = "Send verification code to user's email")
    public ResponseEntity<?> sendResetCode(@RequestBody ForgotPasswordRequest request) {
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

    @PostMapping("/verify-reset-code")
    @Operation(summary = "Verify a password reset code without changing the password")
    public ResponseEntity<?> verifyResetCode(@RequestBody VerifyResetCodeRequest request) {
        if (request == null
                || !StringUtils.hasText(request.getEmail())
                || !StringUtils.hasText(request.getCode())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Email and code are required."
            ));
        }

        try {
            passwordResetService.verifyCode(
                    request.getEmail().trim(),
                    request.getCode().trim()
            );
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Verification code is valid."
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", ex.getMessage()
            ));
        }
    }

    @PostMapping("/reset")
    @Operation(summary = "Reset password after code verification")
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

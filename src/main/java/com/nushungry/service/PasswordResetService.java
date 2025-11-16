package com.nushungry.service;

import com.nushungry.model.PasswordResetToken;
import com.nushungry.model.User;
import com.nushungry.repository.PasswordResetTokenRepository;
import com.nushungry.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${password.reset.code.expiration-minutes:15}")
    private long expirationMinutes;

    private static final int CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public void sendPasswordResetCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User with provided email does not exist."));

        String code = generateVerificationCode();

        passwordResetTokenRepository.deleteByEmail(email);

        PasswordResetToken token = new PasswordResetToken();
        token.setEmail(user.getEmail());
        token.setCode(code);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(expirationMinutes));
        passwordResetTokenRepository.save(token);

        log.info("Generated password reset code for user {}", user.getId());
        emailService.sendPasswordResetCode(user.getEmail(), code);
    }

    @Transactional(readOnly = true)
    public void verifyCode(String email, String code) {
        getValidToken(email, code);
    }

    private String generateVerificationCode() {
        StringBuilder builder = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            int digit = RANDOM.nextInt(10);
            builder.append(digit);
        }
        return builder.toString();
    }

    @Transactional
    public void verifyCodeAndResetPassword(String email, String code, String newPassword) {
        PasswordResetToken token = getValidToken(email, code);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User with provided email does not exist."));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsed(true);
        passwordResetTokenRepository.save(token);

        log.info("Password reset successful for user {}", user.getId());
    }

    public Optional<PasswordResetToken> getLatestTokenForEmail(String email) {
        return passwordResetTokenRepository.findTopByEmailOrderByCreatedAtDesc(email);
    }

    private PasswordResetToken getValidToken(String email, String code) {
        PasswordResetToken token = passwordResetTokenRepository
                .findTopByEmailAndCodeAndUsedFalseOrderByCreatedAtDesc(email, code)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification code."));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Verification code has expired.");
        }

        return token;
    }
}

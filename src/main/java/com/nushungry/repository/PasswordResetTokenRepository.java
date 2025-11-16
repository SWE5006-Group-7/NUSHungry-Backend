package com.nushungry.repository;

import com.nushungry.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findTopByEmailOrderByCreatedAtDesc(String email);
    Optional<PasswordResetToken> findTopByEmailAndCodeAndUsedFalseOrderByCreatedAtDesc(String email, String code);
    void deleteByEmail(String email);
}

package com.goaltracker.repository;

import com.goaltracker.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHashAndUsedAtIsNull(String tokenHash);
    void deleteByUsedAtIsNotNullAndCreatedAtBefore(Instant cutoff);
}


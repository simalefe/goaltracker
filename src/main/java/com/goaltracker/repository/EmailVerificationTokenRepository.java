package com.goaltracker.repository;

import com.goaltracker.model.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByTokenHashAndUsedAtIsNull(String tokenHash);
    void deleteByUsedAtIsNotNullAndCreatedAtBefore(Instant cutoff);
}


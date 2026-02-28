package com.goaltracker.scheduler;

import com.goaltracker.repository.EmailVerificationTokenRepository;
import com.goaltracker.repository.PasswordResetTokenRepository;
import com.goaltracker.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class TokenCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupScheduler.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public TokenCleanupScheduler(RefreshTokenRepository refreshTokenRepository,
                                  EmailVerificationTokenRepository emailVerificationTokenRepository,
                                  PasswordResetTokenRepository passwordResetTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    @Scheduled(cron = "0 0 3 * * *") // Her gün 03:00
    @Transactional
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();

        // Expired refresh tokens
        refreshTokenRepository.deleteByExpiresAtBefore(now);

        // Used email verification tokens (24+ hours old)
        emailVerificationTokenRepository.deleteByUsedAtIsNotNullAndCreatedAtBefore(
                now.minus(24, ChronoUnit.HOURS));

        // Used password reset tokens (1+ hours old)
        passwordResetTokenRepository.deleteByUsedAtIsNotNullAndCreatedAtBefore(
                now.minus(1, ChronoUnit.HOURS));

        log.info("Token temizliği tamamlandı.");
    }
}


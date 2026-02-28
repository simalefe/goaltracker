package com.goaltracker.repository;

import com.goaltracker.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query(value = "UPDATE refresh_tokens SET revoked = true, revoked_at = CURRENT_TIMESTAMP WHERE user_id = :userId AND revoked = false", nativeQuery = true)
    void revokeAllByUserId(@Param("userId") Long userId);

    void deleteByExpiresAtBefore(Instant expiry);
}


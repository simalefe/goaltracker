package com.goaltracker.dto.response;

import java.time.Instant;

public record UserResponse(
    Long id,
    String email,
    String username,
    String displayName,
    String avatarUrl,
    String timezone,
    String role,
    boolean emailVerified,
    Instant createdAt
) {}


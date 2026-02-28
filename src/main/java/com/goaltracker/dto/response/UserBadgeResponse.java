package com.goaltracker.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record UserBadgeResponse(
        BadgeResponse badge,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        Instant earnedAt
) {}


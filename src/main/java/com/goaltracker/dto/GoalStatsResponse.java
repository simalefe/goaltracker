package com.goaltracker.dto;

import java.math.BigDecimal;

public record GoalStatsResponse(
        BigDecimal currentProgress,
        BigDecimal targetValue,
        BigDecimal completionPct,
        BigDecimal expectedPct,
        BigDecimal gap,
        String trackingStatus,
        BigDecimal requiredRate,
        String unit,
        int daysLeft,
        int totalDays,
        int daysSinceStart,
        int entryCount,
        int currentStreak,
        int longestStreak
) {}


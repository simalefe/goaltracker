package com.goaltracker.dto.response;

public record UserStatsResponse(
        long totalEntries,
        long completedGoals,
        int totalStreakDays,
        long earnedBadgeCount
) {}


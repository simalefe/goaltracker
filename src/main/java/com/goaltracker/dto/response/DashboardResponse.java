package com.goaltracker.dto.response;

import java.util.List;

public record DashboardResponse(
        int activeGoalCount,
        int todayEntryCount,
        int totalStreakDays,
        int goalsOnTrack,
        int goalsBehind,
        List<DashboardGoalSummary> topGoals,
        List<RecentEntryResponse> recentEntries
) {}


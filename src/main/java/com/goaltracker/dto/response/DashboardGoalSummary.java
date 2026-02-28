package com.goaltracker.dto.response;

import com.goaltracker.model.enums.GoalCategory;
import com.goaltracker.model.enums.GoalStatus;

import java.math.BigDecimal;

public record DashboardGoalSummary(
        Long goalId,
        String title,
        String unit,
        BigDecimal completionPct,
        BigDecimal gap,
        String trackingStatus,
        GoalStatus status,
        GoalCategory category,
        String color,
        int currentStreak,
        int daysLeft
) {}


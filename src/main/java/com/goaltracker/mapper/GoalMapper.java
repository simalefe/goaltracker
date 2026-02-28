package com.goaltracker.mapper;

import com.goaltracker.dto.CreateGoalRequest;
import com.goaltracker.dto.GoalResponse;
import com.goaltracker.dto.GoalSummaryResponse;
import com.goaltracker.dto.UpdateGoalRequest;
import com.goaltracker.model.Goal;
import com.goaltracker.model.enums.GoalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class GoalMapper {

    private GoalMapper() {}

    public static GoalResponse toResponse(Goal g) {
        if (g == null) return null;
        GoalResponse r = new GoalResponse();
        r.setId(g.getId());
        r.setTitle(g.getTitle());
        r.setDescription(g.getDescription());
        r.setUnit(g.getUnit());
        r.setGoalType(g.getGoalType());
        r.setFrequency(g.getFrequency());
        r.setTargetValue(g.getTargetValue());
        r.setStartDate(g.getStartDate());
        r.setEndDate(g.getEndDate());
        r.setCategory(g.getCategory());
        r.setColor(g.getColor());
        r.setStatus(g.getStatus());
        r.setVersion(g.getVersion());
        r.setCreatedAt(g.getCreatedAt());
        r.setUpdatedAt(g.getUpdatedAt());

        // Computed fields
        r.setDaysLeft(calculateDaysLeft(g));
        r.setCompletionPct(BigDecimal.ZERO);
        r.setCurrentProgress(BigDecimal.ZERO);
        r.setCurrentStreak(0); // Enriched by GoalServiceImpl

        // COMPLETED ise %100
        if (g.getStatus() == GoalStatus.COMPLETED) {
            r.setCompletionPct(new BigDecimal("100.00"));
        }

        return r;
    }

    public static GoalSummaryResponse toSummaryResponse(Goal g) {
        if (g == null) return null;
        GoalSummaryResponse r = new GoalSummaryResponse();
        r.setId(g.getId());
        r.setTitle(g.getTitle());
        r.setUnit(g.getUnit());
        r.setStatus(g.getStatus());
        r.setCategory(g.getCategory());
        r.setColor(g.getColor());
        r.setEndDate(g.getEndDate());
        r.setDaysLeft(calculateDaysLeft(g));
        r.setCompletionPct(BigDecimal.ZERO);
        r.setCurrentStreak(0); // Enriched by GoalServiceImpl

        if (g.getStatus() == GoalStatus.COMPLETED) {
            r.setCompletionPct(new BigDecimal("100.00"));
        }

        return r;
    }

    public static Goal toEntity(CreateGoalRequest req) {
        if (req == null) return null;
        Goal g = new Goal();
        g.setTitle(req.getTitle());
        g.setDescription(req.getDescription());
        g.setUnit(req.getUnit());
        g.setGoalType(req.getGoalType());
        g.setFrequency(req.getFrequency() != null ? req.getFrequency() : com.goaltracker.model.enums.GoalFrequency.DAILY);
        g.setTargetValue(req.getTargetValue());
        g.setStartDate(req.getStartDate());
        g.setEndDate(req.getEndDate());
        g.setCategory(req.getCategory());
        g.setColor(req.getColor());
        return g;
    }

    public static void updateEntityFromDto(UpdateGoalRequest req, Goal g) {
        if (req.getTitle() != null) g.setTitle(req.getTitle());
        if (req.getDescription() != null) g.setDescription(req.getDescription());
        if (req.getUnit() != null) g.setUnit(req.getUnit());
        if (req.getGoalType() != null) g.setGoalType(req.getGoalType());
        if (req.getFrequency() != null) g.setFrequency(req.getFrequency());
        if (req.getTargetValue() != null) g.setTargetValue(req.getTargetValue());
        if (req.getStartDate() != null) g.setStartDate(req.getStartDate());
        if (req.getEndDate() != null) g.setEndDate(req.getEndDate());
        if (req.getCategory() != null) g.setCategory(req.getCategory());
        if (req.getColor() != null) g.setColor(req.getColor());
    }

    private static int calculateDaysLeft(Goal g) {
        if (g.getEndDate() == null) return 0;
        long days = ChronoUnit.DAYS.between(LocalDate.now(), g.getEndDate());
        return (int) Math.max(0, days);
    }
}


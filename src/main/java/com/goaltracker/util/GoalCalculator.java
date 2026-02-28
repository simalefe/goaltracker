package com.goaltracker.util;

import com.goaltracker.dto.ChartDataPointResponse;
import com.goaltracker.model.Goal;
import com.goaltracker.model.GoalEntry;
import com.goaltracker.model.enums.GoalType;
import com.goaltracker.repository.GoalEntryRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class GoalCalculator {

    private final GoalEntryRepository goalEntryRepository;

    public GoalCalculator(GoalEntryRepository goalEntryRepository) {
        this.goalEntryRepository = goalEntryRepository;
    }

    /**
     * SUM(actual_value) for a goal — null-safe via COALESCE.
     */
    public BigDecimal calculateCurrentProgress(Long goalId) {
        return goalEntryRepository.sumActualValueByGoalId(goalId);
    }

    /**
     * Planned (expected) progress as of a given date.
     */
    public BigDecimal calculatePlannedProgress(Goal goal, LocalDate asOfDate) {
        long totalDays = calculateTotalDays(goal);
        if (totalDays <= 0) return BigDecimal.ZERO;

        if (goal.getGoalType() == GoalType.RATE) {
            long elapsedPeriods = calculateElapsedPeriods(goal, asOfDate);
            return goal.getTargetValue().multiply(BigDecimal.valueOf(elapsedPeriods));
        }

        // DAILY / CUMULATIVE
        long daysSinceStart = ChronoUnit.DAYS.between(goal.getStartDate(), asOfDate) + 1;
        daysSinceStart = Math.max(0, Math.min(daysSinceStart, totalDays));

        return goal.getTargetValue()
                .multiply(BigDecimal.valueOf(daysSinceStart))
                .divide(BigDecimal.valueOf(totalDays), 4, RoundingMode.HALF_UP);
    }

    public long calculateTotalDays(Goal goal) {
        if (goal.getStartDate() == null || goal.getEndDate() == null) return 0;
        long days = ChronoUnit.DAYS.between(goal.getStartDate(), goal.getEndDate()) + 1;
        return Math.max(1, days);
    }

    public long calculateTotalPeriods(Goal goal) {
        long totalDays = calculateTotalDays(goal);
        return switch (goal.getFrequency()) {
            case DAILY -> totalDays;
            case WEEKLY -> (totalDays + 6) / 7;
            case MONTHLY -> ChronoUnit.MONTHS.between(goal.getStartDate(), goal.getEndDate()) + 1;
        };
    }

    public long calculateElapsedPeriods(Goal goal, LocalDate asOfDate) {
        long daysSinceStart = ChronoUnit.DAYS.between(goal.getStartDate(), asOfDate) + 1;
        daysSinceStart = Math.max(0, daysSinceStart);
        return switch (goal.getFrequency()) {
            case DAILY -> daysSinceStart;
            case WEEKLY -> (daysSinceStart + 6) / 7;
            case MONTHLY -> {
                if (daysSinceStart <= 0) yield 0L;
                yield ChronoUnit.MONTHS.between(goal.getStartDate(), asOfDate) + 1;
            }
        };
    }

    /**
     * Gap = currentProgress - plannedProgress (positive = ahead, negative = behind).
     */
    public BigDecimal calculateGap(Goal goal) {
        BigDecimal current = calculateCurrentProgress(goal.getId());
        BigDecimal planned = calculatePlannedProgress(goal, LocalDate.now());
        return current.subtract(planned);
    }

    /**
     * Required daily rate to finish on time. Safe against division by zero.
     */
    public BigDecimal calculateRequiredRate(Goal goal) {
        long daysLeft = calculateDaysLeft(goal);
        if (daysLeft <= 0) return BigDecimal.ZERO;

        BigDecimal current = calculateCurrentProgress(goal.getId());
        BigDecimal remaining = goal.getTargetValue().subtract(current);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;

        return remaining.divide(BigDecimal.valueOf(daysLeft), 2, RoundingMode.HALF_UP);
    }

    public int calculateDaysLeft(Goal goal) {
        if (goal.getEndDate() == null) return 0;
        long days = ChronoUnit.DAYS.between(LocalDate.now(), goal.getEndDate());
        return (int) Math.max(0, days);
    }

    public int calculateDaysSinceStart(Goal goal) {
        if (goal.getStartDate() == null) return 0;
        long days = ChronoUnit.DAYS.between(goal.getStartDate(), LocalDate.now()) + 1;
        return (int) Math.max(0, days);
    }

    /**
     * Completion percentage, capped at 100.00.
     */
    public BigDecimal calculateCompletionPct(Goal goal) {
        if (goal.getTargetValue() == null || goal.getTargetValue().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal current = calculateCurrentProgress(goal.getId());
        return current
                .divide(goal.getTargetValue(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .min(new BigDecimal("100.00"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Expected percentage based on elapsed time.
     */
    public BigDecimal calculateExpectedPct(Goal goal) {
        long totalDays = calculateTotalDays(goal);
        if (totalDays <= 0) return BigDecimal.ZERO;

        long daysSinceStart = ChronoUnit.DAYS.between(goal.getStartDate(), LocalDate.now()) + 1;
        daysSinceStart = Math.max(0, Math.min(daysSinceStart, totalDays));

        return BigDecimal.valueOf(daysSinceStart)
                .divide(BigDecimal.valueOf(totalDays), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Determines tracking status: AHEAD, ON_TRACK, or BEHIND.
     */
    public String determineTrackingStatus(Goal goal) {
        BigDecimal gap = calculateGap(goal);
        if (gap.compareTo(BigDecimal.ZERO) > 0) return "AHEAD";
        if (gap.compareTo(BigDecimal.ZERO) == 0) return "ON_TRACK";
        return "BEHIND";
    }

    /**
     * Builds chart data points from startDate to min(today, endDate).
     */
    public List<ChartDataPointResponse> buildChartData(Goal goal, List<GoalEntry> entries) {
        LocalDate start = goal.getStartDate();
        LocalDate end = goal.getEndDate();
        LocalDate today = LocalDate.now();
        LocalDate chartEnd = end.isBefore(today) ? end : today;

        if (start.isAfter(chartEnd)) {
            return List.of();
        }

        long totalDays = calculateTotalDays(goal);

        // Build entry lookup map
        Map<LocalDate, BigDecimal> entryMap = entries.stream()
                .collect(Collectors.toMap(GoalEntry::getEntryDate, GoalEntry::getActualValue));

        List<ChartDataPointResponse> dataPoints = new ArrayList<>();
        BigDecimal cumulativeActual = BigDecimal.ZERO;
        boolean hasAnyEntry = false;

        for (LocalDate date = start; !date.isAfter(chartEnd); date = date.plusDays(1)) {
            // Calculate cumulative planned
            long dayIndex = ChronoUnit.DAYS.between(start, date) + 1;
            BigDecimal planned;
            if (goal.getGoalType() == GoalType.RATE) {
                long elapsed = calculateElapsedPeriods(goal, date);
                planned = goal.getTargetValue().multiply(BigDecimal.valueOf(elapsed));
            } else {
                planned = goal.getTargetValue()
                        .multiply(BigDecimal.valueOf(dayIndex))
                        .divide(BigDecimal.valueOf(totalDays), 2, RoundingMode.HALF_UP);
            }

            BigDecimal dailyActual = entryMap.get(date);
            if (dailyActual != null) {
                cumulativeActual = cumulativeActual.add(dailyActual);
                hasAnyEntry = true;
            }

            dataPoints.add(new ChartDataPointResponse(
                    date.toString(),
                    planned,
                    hasAnyEntry ? cumulativeActual : null,
                    dailyActual
            ));
        }

        return dataPoints;
    }
}


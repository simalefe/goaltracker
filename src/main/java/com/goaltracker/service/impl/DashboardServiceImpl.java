package com.goaltracker.service.impl;

import com.goaltracker.dto.response.DashboardGoalSummary;
import com.goaltracker.dto.response.DashboardResponse;
import com.goaltracker.dto.response.RecentEntryResponse;
import com.goaltracker.model.Goal;
import com.goaltracker.model.GoalEntry;
import com.goaltracker.model.enums.GoalStatus;
import com.goaltracker.repository.GoalEntryRepository;
import com.goaltracker.repository.GoalRepository;
import com.goaltracker.service.DashboardService;
import com.goaltracker.service.StreakService;
import com.goaltracker.util.GoalCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);

    private final GoalRepository goalRepository;
    private final GoalEntryRepository goalEntryRepository;
    private final GoalCalculator goalCalculator;
    private final StreakService streakService;

    public DashboardServiceImpl(GoalRepository goalRepository,
                                 GoalEntryRepository goalEntryRepository,
                                 GoalCalculator goalCalculator,
                                 StreakService streakService) {
        this.goalRepository = goalRepository;
        this.goalEntryRepository = goalEntryRepository;
        this.goalCalculator = goalCalculator;
        this.streakService = streakService;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "dashboard", key = "#userId")
    public DashboardResponse getDashboard(Long userId) {
        log.debug("Dashboard hesaplanıyor: userId={}", userId);

        // 1. Aktif hedefleri tek sorguda çek
        List<Goal> activeGoals = goalRepository.findByUserIdAndStatus(userId, GoalStatus.ACTIVE);

        if (activeGoals.isEmpty()) {
            return new DashboardResponse(0, 0, 0, 0, 0, List.of(), List.of());
        }

        // 2. Tüm aktif hedeflerin progress'ini batch aggregate sorguda çek
        List<Long> goalIds = activeGoals.stream().map(Goal::getId).toList();
        Map<Long, BigDecimal> goalProgressMap = new HashMap<>();

        if (!goalIds.isEmpty()) {
            List<Object[]> progressData = goalEntryRepository.sumByGoalIds(goalIds);
            for (Object[] row : progressData) {
                Long goalId = (Long) row[0];
                BigDecimal total = (BigDecimal) row[1];
                goalProgressMap.put(goalId, total);
            }
        }

        // 3. Bugünün entry sayısı (distinct goal)
        int todayEntryCount = goalEntryRepository.countDistinctGoalsByUserIdAndEntryDate(userId, LocalDate.now());

        // 4. GoalCalculator ile on-track/behind hesapla + topGoals oluştur
        int onTrack = 0;
        int behind = 0;
        List<DashboardGoalSummary> allSummaries = new ArrayList<>();

        for (Goal goal : activeGoals) {
            BigDecimal progress = goalProgressMap.getOrDefault(goal.getId(), BigDecimal.ZERO);
            BigDecimal planned = goalCalculator.calculatePlannedProgress(goal, LocalDate.now());
            BigDecimal gap = progress.subtract(planned);

            if (gap.compareTo(BigDecimal.ZERO) >= 0) {
                onTrack++;
            } else {
                behind++;
            }

            // Completion percentage
            BigDecimal completionPct = BigDecimal.ZERO;
            if (goal.getTargetValue() != null && goal.getTargetValue().compareTo(BigDecimal.ZERO) > 0) {
                completionPct = progress
                        .divide(goal.getTargetValue(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .min(new BigDecimal("100.00"))
                        .setScale(2, RoundingMode.HALF_UP);
            }

            // Tracking status
            String trackingStatus;
            if (gap.compareTo(BigDecimal.ZERO) > 0) {
                trackingStatus = "AHEAD";
            } else if (gap.compareTo(BigDecimal.ZERO) == 0) {
                trackingStatus = "ON_TRACK";
            } else {
                trackingStatus = "BEHIND";
            }

            int daysLeft = goalCalculator.calculateDaysLeft(goal);
            int goalCurrentStreak = streakService.getStreakForGoal(goal.getId());

            allSummaries.add(new DashboardGoalSummary(
                    goal.getId(),
                    goal.getTitle(),
                    goal.getUnit(),
                    completionPct,
                    gap.setScale(2, RoundingMode.HALF_UP),
                    trackingStatus,
                    goal.getStatus(),
                    goal.getCategory(),
                    goal.getColor(),
                    goalCurrentStreak,
                    daysLeft
            ));
        }

        // Top 5 goals by completionPct DESC
        List<DashboardGoalSummary> topGoals = allSummaries.stream()
                .sorted(Comparator.comparing(DashboardGoalSummary::completionPct).reversed())
                .limit(5)
                .toList();

        // 5. Son 5 entry
        List<GoalEntry> recentEntryEntities = goalEntryRepository.findRecentByUserId(userId, PageRequest.of(0, 5));
        List<RecentEntryResponse> recentEntries = recentEntryEntities.stream()
                .map(entry -> new RecentEntryResponse(
                        entry.getId(),
                        entry.getGoal().getId(),
                        entry.getGoal().getTitle(),
                        entry.getGoal().getUnit(),
                        entry.getActualValue(),
                        entry.getEntryDate(),
                        entry.getCreatedAt()
                ))
                .toList();

        log.debug("Dashboard hazır: userId={}, activeGoals={}, onTrack={}, behind={}",
                userId, activeGoals.size(), onTrack, behind);

        return new DashboardResponse(
                activeGoals.size(),
                todayEntryCount,
                streakService.getTotalStreakDays(userId),
                onTrack,
                behind,
                topGoals,
                recentEntries
        );
    }
}


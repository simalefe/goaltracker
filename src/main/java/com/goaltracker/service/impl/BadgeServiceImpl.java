package com.goaltracker.service.impl;

import com.goaltracker.dto.response.BadgeResponse;
import com.goaltracker.dto.response.UserBadgeResponse;
import com.goaltracker.model.Badge;
import com.goaltracker.model.User;
import com.goaltracker.model.UserBadge;
import com.goaltracker.model.event.BadgeEarnedEvent;
import com.goaltracker.repository.*;
import com.goaltracker.service.BadgeService;
import com.goaltracker.service.StreakService;
import com.goaltracker.util.GoalCalculator;
import com.goaltracker.model.Goal;
import com.goaltracker.model.enums.GoalStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class BadgeServiceImpl implements BadgeService {

    private static final Logger log = LoggerFactory.getLogger(BadgeServiceImpl.class);

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final GoalEntryRepository goalEntryRepository;
    private final GoalRepository goalRepository;
    private final StreakService streakService;
    private final GoalCalculator goalCalculator;
    private final ApplicationEventPublisher eventPublisher;

    public BadgeServiceImpl(BadgeRepository badgeRepository,
                            UserBadgeRepository userBadgeRepository,
                            GoalEntryRepository goalEntryRepository,
                            GoalRepository goalRepository,
                            StreakService streakService,
                            GoalCalculator goalCalculator,
                            ApplicationEventPublisher eventPublisher) {
        this.badgeRepository = badgeRepository;
        this.userBadgeRepository = userBadgeRepository;
        this.goalEntryRepository = goalEntryRepository;
        this.goalRepository = goalRepository;
        this.streakService = streakService;
        this.goalCalculator = goalCalculator;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void checkAndAwardBadges(Long userId, Long goalId) {
        // 1. STREAK badges
        int currentStreak = streakService.getStreakForGoal(goalId);
        checkStreakBadges(userId, currentStreak);

        // 2. ENTRY_COUNT badges
        checkEntryCountBadges(userId);

        // 3. COMPLETIONS badges
        checkCompletionBadges(userId);

        // 4. ACTIVE_GOALS badges
        checkActiveGoalsBadges(userId);

        // 5. PACE_PCT badges
        checkPaceBadges(userId, goalId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserBadgeResponse> getUserBadges(Long userId) {
        return userBadgeRepository.findByUserId(userId).stream()
                .map(ub -> new UserBadgeResponse(toBadgeResponse(ub.getBadge()), ub.getEarnedAt()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long getUserBadgeCount(Long userId) {
        return userBadgeRepository.countByUserId(userId);
    }

    private void checkStreakBadges(Long userId, int maxStreak) {
        List<Badge> streakBadges = badgeRepository.findByConditionType("STREAK");
        for (Badge badge : streakBadges) {
            if (maxStreak >= badge.getConditionValue()) {
                awardBadge(userId, badge);
            }
        }
    }

    private void checkEntryCountBadges(Long userId) {
        long totalEntries = goalEntryRepository.countByGoal_User_Id(userId);
        List<Badge> entryBadges = badgeRepository.findByConditionType("ENTRY_COUNT");
        for (Badge badge : entryBadges) {
            if (totalEntries >= badge.getConditionValue()) {
                awardBadge(userId, badge);
            }
        }
    }

    private void checkCompletionBadges(Long userId) {
        long completedGoals = goalRepository.countByUserIdAndStatus(userId, GoalStatus.COMPLETED);
        List<Badge> completionBadges = badgeRepository.findByConditionType("COMPLETIONS");
        for (Badge badge : completionBadges) {
            if (completedGoals >= badge.getConditionValue()) {
                awardBadge(userId, badge);
            }
        }
    }

    private void checkActiveGoalsBadges(Long userId) {
        long activeGoals = goalRepository.countByUserIdAndStatus(userId, GoalStatus.ACTIVE);
        List<Badge> activeGoalBadges = badgeRepository.findByConditionType("ACTIVE_GOALS");
        for (Badge badge : activeGoalBadges) {
            if (activeGoals >= badge.getConditionValue()) {
                awardBadge(userId, badge);
            }
        }
    }

    private void checkPaceBadges(Long userId, Long goalId) {
        Goal goal = goalRepository.findById(goalId).orElse(null);
        if (goal == null) return;

        BigDecimal currentProgress = goalCalculator.calculateCurrentProgress(goalId);
        BigDecimal plannedProgress = goalCalculator.calculatePlannedProgress(goal, java.time.LocalDate.now());

        if (plannedProgress.compareTo(BigDecimal.ZERO) <= 0) return;

        BigDecimal pace = currentProgress
                .divide(plannedProgress, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        List<Badge> paceBadges = badgeRepository.findByConditionType("PACE_PCT");
        for (Badge badge : paceBadges) {
            if (pace.compareTo(BigDecimal.valueOf(badge.getConditionValue())) >= 0) {
                awardBadge(userId, badge);
            }
        }
    }

    private void awardBadge(Long userId, Badge badge) {
        // Idempotency check — zaten kazanıldıysa sessizce dön
        if (userBadgeRepository.existsByUserIdAndBadgeId(userId, badge.getId())) {
            return;
        }

        UserBadge userBadge = new UserBadge();
        User user = new User();
        user.setId(userId);
        userBadge.setUser(user);
        userBadge.setBadge(badge);
        userBadgeRepository.save(userBadge);

        log.info("Rozet kazanıldı! userId={}, badge={} ({})", userId, badge.getCode(), badge.getName());

        // Faz 6 için event publish
        eventPublisher.publishEvent(new BadgeEarnedEvent(this, userId, badge.getId(), badge.getCode()));
    }

    private BadgeResponse toBadgeResponse(Badge badge) {
        return new BadgeResponse(
                badge.getId(),
                badge.getCode(),
                badge.getName(),
                badge.getDescription(),
                badge.getIcon(),
                badge.getConditionType(),
                badge.getConditionValue()
        );
    }
}


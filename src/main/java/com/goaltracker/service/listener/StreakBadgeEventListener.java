package com.goaltracker.service.listener;

import com.goaltracker.model.event.GoalEntryCreatedEvent;
import com.goaltracker.service.BadgeService;
import com.goaltracker.service.StreakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class StreakBadgeEventListener {

    private static final Logger log = LoggerFactory.getLogger(StreakBadgeEventListener.class);

    private final StreakService streakService;
    private final BadgeService badgeService;

    public StreakBadgeEventListener(StreakService streakService, BadgeService badgeService) {
        this.streakService = streakService;
        this.badgeService = badgeService;
    }

    @Async
    @EventListener
    public void handleGoalEntryCreated(GoalEntryCreatedEvent event) {
        try {
            log.debug("GoalEntryCreatedEvent alındı: goalId={}, userId={}, date={}",
                    event.getGoalId(), event.getUserId(), event.getEntryDate());

            // 1. Streak güncelle
            streakService.updateStreak(event.getGoalId(), event.getEntryDate());

            // 2. Badge kontrol et ve ver
            badgeService.checkAndAwardBadges(event.getUserId(), event.getGoalId());

        } catch (Exception e) {
            // Streak/badge hatası entry kaydını etkilememeli
            log.error("Streak/Badge işlemi sırasında hata: goalId={}, userId={}, error={}",
                    event.getGoalId(), event.getUserId(), e.getMessage(), e);
        }
    }
}


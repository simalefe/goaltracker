package com.goaltracker.service.listener;

import com.goaltracker.model.Badge;
import com.goaltracker.model.enums.NotificationType;
import com.goaltracker.model.event.BadgeEarnedEvent;
import com.goaltracker.model.event.GoalCompletedEvent;
import com.goaltracker.repository.BadgeRepository;
import com.goaltracker.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;
    private final BadgeRepository badgeRepository;

    public NotificationEventListener(NotificationService notificationService,
                                      BadgeRepository badgeRepository) {
        this.notificationService = notificationService;
        this.badgeRepository = badgeRepository;
    }

    @Async
    @EventListener
    public void handleBadgeEarned(BadgeEarnedEvent event) {
        try {
            log.debug("BadgeEarnedEvent alındı: userId={}, badgeCode={}", event.getUserId(), event.getBadgeCode());

            String badgeName = badgeRepository.findById(event.getBadgeId())
                    .map(Badge::getName)
                    .orElse(event.getBadgeCode());

            String title = "🏆 Yeni Rozet Kazandınız!";
            String message = "Tebrikler! \"" + badgeName + "\" rozetini kazandınız.";
            Map<String, Object> metadata = Map.of(
                    "badgeId", event.getBadgeId(),
                    "badgeCode", event.getBadgeCode()
            );

            notificationService.createNotification(
                    event.getUserId(), NotificationType.BADGE_EARNED, title, message, metadata);

        } catch (Exception e) {
            log.error("Badge bildirim hatası: userId={}, error={}", event.getUserId(), e.getMessage(), e);
        }
    }

    @Async
    @EventListener
    public void handleGoalCompleted(GoalCompletedEvent event) {
        try {
            log.debug("GoalCompletedEvent alındı: userId={}, goalId={}", event.getUserId(), event.getGoalId());

            String title = "✅ Hedef Tamamlandı!";
            String message = "Tebrikler! \"" + event.getGoalTitle() + "\" hedefinizi başarıyla tamamladınız.";
            Map<String, Object> metadata = Map.of("goalId", event.getGoalId());

            notificationService.createNotification(
                    event.getUserId(), NotificationType.GOAL_COMPLETED, title, message, metadata);

        } catch (Exception e) {
            log.error("Goal completed bildirim hatası: userId={}, error={}", event.getUserId(), e.getMessage(), e);
        }
    }
}


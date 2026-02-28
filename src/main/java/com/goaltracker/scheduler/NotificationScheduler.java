package com.goaltracker.scheduler;

import com.goaltracker.model.Goal;
import com.goaltracker.model.NotificationSettings;
import com.goaltracker.model.Streak;
import com.goaltracker.model.enums.GoalStatus;
import com.goaltracker.model.enums.NotificationType;
import com.goaltracker.repository.GoalEntryRepository;
import com.goaltracker.repository.GoalRepository;
import com.goaltracker.repository.NotificationSettingsRepository;
import com.goaltracker.repository.StreakRepository;
import com.goaltracker.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Component
public class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);

    private final NotificationService notificationService;
    private final NotificationSettingsRepository settingsRepository;
    private final GoalRepository goalRepository;
    private final GoalEntryRepository goalEntryRepository;
    private final StreakRepository streakRepository;

    public NotificationScheduler(NotificationService notificationService,
                                  NotificationSettingsRepository settingsRepository,
                                  GoalRepository goalRepository,
                                  GoalEntryRepository goalEntryRepository,
                                  StreakRepository streakRepository) {
        this.notificationService = notificationService;
        this.settingsRepository = settingsRepository;
        this.goalRepository = goalRepository;
        this.goalEntryRepository = goalEntryRepository;
        this.streakRepository = streakRepository;
    }

    /**
     * Her dakika çalışır — o dakikaya daily reminder ayarlanmış kullanıcıları bulur.
     */
    @Scheduled(cron = "0 * * * * *")
    public void sendDailyReminders() {
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);
        log.debug("Daily reminder kontrolü: time={}", now);

        try {
            List<NotificationSettings> settingsList = settingsRepository.findByDailyReminderTime(now);
            LocalDate today = LocalDate.now();

            for (NotificationSettings settings : settingsList) {
                Long userId = settings.getUser().getId();

                // Bugün zaten gönderildi mi?
                if (notificationService.alreadySentToday(userId, NotificationType.DAILY_REMINDER)) {
                    continue;
                }

                // Aktif hedefi var mı?
                List<Goal> activeGoals = goalRepository.findByUserIdAndStatus(userId, GoalStatus.ACTIVE);
                if (activeGoals.isEmpty()) {
                    continue;
                }

                // Bugün entry girilmiş hedef var mı?
                int todayEntryCount = goalEntryRepository.countDistinctGoalsByUserIdAndEntryDate(userId, today);
                if (todayEntryCount >= activeGoals.size()) {
                    continue; // Tüm hedefler için entry girilmiş
                }

                int remaining = activeGoals.size() - todayEntryCount;
                String title = "⏰ Günlük Hatırlatma";
                String message = "Bugün " + remaining + " hedefiniz için henüz ilerleme kaydetmediniz. Hedeflerinize göz atın!";

                notificationService.createNotification(
                        userId, NotificationType.DAILY_REMINDER, title, message, null);
            }
        } catch (Exception e) {
            log.error("Daily reminder scheduler hatası: {}", e.getMessage(), e);
        }
    }

    /**
     * Saat 22:00 — O gün entry girilmemiş, streak > 0 olan ACTIVE hedefler için uyarı.
     */
    @Scheduled(cron = "0 0 22 * * *")
    public void sendStreakDangerWarnings() {
        log.info("Streak danger uyarı scheduler'ı başlatıldı.");

        try {
            List<NotificationSettings> settingsList = settingsRepository.findByStreakDangerEnabledTrue();
            LocalDate today = LocalDate.now();

            for (NotificationSettings settings : settingsList) {
                Long userId = settings.getUser().getId();

                // Bugün zaten gönderildi mi?
                if (notificationService.alreadySentToday(userId, NotificationType.STREAK_DANGER)) {
                    continue;
                }

                List<Goal> activeGoals = goalRepository.findByUserIdAndStatus(userId, GoalStatus.ACTIVE);

                for (Goal goal : activeGoals) {
                    // Bugün entry var mı?
                    boolean hasEntryToday = goalEntryRepository.existsByGoalIdAndEntryDate(goal.getId(), today);
                    if (hasEntryToday) {
                        continue;
                    }

                    // Streak > 0 mı?
                    Streak streak = streakRepository.findByGoalId(goal.getId()).orElse(null);
                    if (streak == null || streak.getCurrentStreak() == 0) {
                        continue;
                    }

                    String title = "⚠️ Streak Tehlikede!";
                    String message = "\"" + goal.getTitle() + "\" hedefinizdeki " + streak.getCurrentStreak()
                            + " günlük seriyi kaybetmek üzeresiniz. Bugün ilerlemenizi kaydedin!";
                    Map<String, Object> metadata = Map.of(
                            "goalId", goal.getId(),
                            "currentStreak", streak.getCurrentStreak()
                    );

                    notificationService.createNotification(
                            userId, NotificationType.STREAK_DANGER, title, message, metadata);
                    break; // Aynı kullanıcıya bugün tek STREAK_DANGER bildirimi
                }
            }

            log.info("Streak danger uyarıları tamamlandı.");
        } catch (Exception e) {
            log.error("Streak danger scheduler hatası: {}", e.getMessage(), e);
        }
    }

    /**
     * Her Pazartesi 09:00 — Haftalık özet gönder.
     */
    @Scheduled(cron = "0 0 9 * * MON")
    public void sendWeeklySummaries() {
        log.info("Haftalık özet scheduler'ı başlatıldı.");

        try {
            List<NotificationSettings> settingsList = settingsRepository.findByWeeklySummaryEnabledTrue();
            LocalDate today = LocalDate.now();
            LocalDate weekStart = today.minusDays(7);

            for (NotificationSettings settings : settingsList) {
                Long userId = settings.getUser().getId();

                // Bugün zaten gönderildi mi?
                if (notificationService.alreadySentToday(userId, NotificationType.WEEKLY_SUMMARY)) {
                    continue;
                }

                List<Goal> activeGoals = goalRepository.findByUserIdAndStatus(userId, GoalStatus.ACTIVE);
                if (activeGoals.isEmpty()) {
                    continue;
                }

                // Geçen haftanın özet verileri
                int totalEntries = 0;
                int goalsWithEntries = 0;
                int maxStreak = 0;

                for (Goal goal : activeGoals) {
                    long entryCount = goalEntryRepository.countByGoalIdAndEntryDateBetween(
                            goal.getId(), weekStart, today.minusDays(1));
                    if (entryCount > 0) {
                        goalsWithEntries++;
                        totalEntries += (int) entryCount;
                    }
                    Streak streak = streakRepository.findByGoalId(goal.getId()).orElse(null);
                    if (streak != null && streak.getCurrentStreak() > maxStreak) {
                        maxStreak = streak.getCurrentStreak();
                    }
                }

                int completionPct = activeGoals.isEmpty() ? 0
                        : (int) ((goalsWithEntries * 100.0) / activeGoals.size());

                String title = "📊 Haftalık Özet";
                String message = String.format(
                        "Geçen hafta: %d ilerleme kaydı, %d/%d hedefe katkı (%%%d), en uzun seri: %d gün.",
                        totalEntries, goalsWithEntries, activeGoals.size(), completionPct, maxStreak);
                Map<String, Object> metadata = Map.of(
                        "totalEntries", totalEntries,
                        "goalsWithEntries", goalsWithEntries,
                        "totalGoals", activeGoals.size(),
                        "completionPct", completionPct,
                        "maxStreak", maxStreak
                );

                notificationService.createNotification(
                        userId, NotificationType.WEEKLY_SUMMARY, title, message, metadata);
            }

            log.info("Haftalık özetler tamamlandı.");
        } catch (Exception e) {
            log.error("Haftalık özet scheduler hatası: {}", e.getMessage(), e);
        }
    }
}


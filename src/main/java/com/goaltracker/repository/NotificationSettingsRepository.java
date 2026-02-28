package com.goaltracker.repository;

import com.goaltracker.model.NotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface NotificationSettingsRepository extends JpaRepository<NotificationSettings, Long> {

    Optional<NotificationSettings> findByUserId(Long userId);

    @Query("SELECT ns FROM NotificationSettings ns WHERE ns.dailyReminderTime = :time")
    List<NotificationSettings> findByDailyReminderTime(@Param("time") LocalTime time);

    @Query("SELECT ns FROM NotificationSettings ns WHERE ns.weeklySummaryEnabled = true")
    List<NotificationSettings> findByWeeklySummaryEnabledTrue();

    @Query("SELECT ns FROM NotificationSettings ns WHERE ns.streakDangerEnabled = true")
    List<NotificationSettings> findByStreakDangerEnabledTrue();
}


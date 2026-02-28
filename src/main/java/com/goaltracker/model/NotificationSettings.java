package com.goaltracker.model;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "notification_settings")
public class NotificationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = true;

    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled = true;

    @Column(name = "daily_reminder_time", nullable = false)
    private LocalTime dailyReminderTime = LocalTime.of(20, 0);

    @Column(name = "weekly_summary_day", nullable = false)
    private int weeklySummaryDay = 1;

    @Column(name = "weekly_summary_enabled", nullable = false)
    private boolean weeklySummaryEnabled = true;

    @Column(name = "streak_danger_enabled", nullable = false)
    private boolean streakDangerEnabled = true;

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public boolean isEmailEnabled() { return emailEnabled; }
    public void setEmailEnabled(boolean emailEnabled) { this.emailEnabled = emailEnabled; }

    public boolean isPushEnabled() { return pushEnabled; }
    public void setPushEnabled(boolean pushEnabled) { this.pushEnabled = pushEnabled; }

    public LocalTime getDailyReminderTime() { return dailyReminderTime; }
    public void setDailyReminderTime(LocalTime dailyReminderTime) { this.dailyReminderTime = dailyReminderTime; }

    public int getWeeklySummaryDay() { return weeklySummaryDay; }
    public void setWeeklySummaryDay(int weeklySummaryDay) { this.weeklySummaryDay = weeklySummaryDay; }

    public boolean isWeeklySummaryEnabled() { return weeklySummaryEnabled; }
    public void setWeeklySummaryEnabled(boolean weeklySummaryEnabled) { this.weeklySummaryEnabled = weeklySummaryEnabled; }

    public boolean isStreakDangerEnabled() { return streakDangerEnabled; }
    public void setStreakDangerEnabled(boolean streakDangerEnabled) { this.streakDangerEnabled = streakDangerEnabled; }
}


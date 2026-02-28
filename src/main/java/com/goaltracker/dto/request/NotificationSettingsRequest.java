package com.goaltracker.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalTime;

public class NotificationSettingsRequest {

    private Boolean emailEnabled;
    private Boolean pushEnabled;
    private LocalTime dailyReminderTime;

    @Min(1)
    @Max(7)
    private Integer weeklySummaryDay;

    private Boolean weeklySummaryEnabled;
    private Boolean streakDangerEnabled;

    public NotificationSettingsRequest() {}

    // --- Getters & Setters ---
    public Boolean getEmailEnabled() { return emailEnabled; }
    public void setEmailEnabled(Boolean emailEnabled) { this.emailEnabled = emailEnabled; }

    public Boolean getPushEnabled() { return pushEnabled; }
    public void setPushEnabled(Boolean pushEnabled) { this.pushEnabled = pushEnabled; }

    public LocalTime getDailyReminderTime() { return dailyReminderTime; }
    public void setDailyReminderTime(LocalTime dailyReminderTime) { this.dailyReminderTime = dailyReminderTime; }

    public Integer getWeeklySummaryDay() { return weeklySummaryDay; }
    public void setWeeklySummaryDay(Integer weeklySummaryDay) { this.weeklySummaryDay = weeklySummaryDay; }

    public Boolean getWeeklySummaryEnabled() { return weeklySummaryEnabled; }
    public void setWeeklySummaryEnabled(Boolean weeklySummaryEnabled) { this.weeklySummaryEnabled = weeklySummaryEnabled; }

    public Boolean getStreakDangerEnabled() { return streakDangerEnabled; }
    public void setStreakDangerEnabled(Boolean streakDangerEnabled) { this.streakDangerEnabled = streakDangerEnabled; }
}


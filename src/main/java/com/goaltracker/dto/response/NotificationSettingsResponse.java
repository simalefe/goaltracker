package com.goaltracker.dto.response;

import java.time.LocalTime;

public class NotificationSettingsResponse {

    private boolean emailEnabled;
    private boolean pushEnabled;
    private LocalTime dailyReminderTime;
    private int weeklySummaryDay;
    private boolean weeklySummaryEnabled;
    private boolean streakDangerEnabled;

    public NotificationSettingsResponse() {}

    public NotificationSettingsResponse(boolean emailEnabled, boolean pushEnabled,
                                         LocalTime dailyReminderTime, int weeklySummaryDay,
                                         boolean weeklySummaryEnabled, boolean streakDangerEnabled) {
        this.emailEnabled = emailEnabled;
        this.pushEnabled = pushEnabled;
        this.dailyReminderTime = dailyReminderTime;
        this.weeklySummaryDay = weeklySummaryDay;
        this.weeklySummaryEnabled = weeklySummaryEnabled;
        this.streakDangerEnabled = streakDangerEnabled;
    }

    // --- Getters & Setters ---
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

    /**
     * Helper: returns the Turkish day name for weeklySummaryDay.
     */
    public String getWeeklySummaryDayName() {
        return switch (weeklySummaryDay) {
            case 1 -> "Pazartesi";
            case 2 -> "Salı";
            case 3 -> "Çarşamba";
            case 4 -> "Perşembe";
            case 5 -> "Cuma";
            case 6 -> "Cumartesi";
            case 7 -> "Pazar";
            default -> "Pazartesi";
        };
    }
}


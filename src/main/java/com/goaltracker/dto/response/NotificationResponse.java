package com.goaltracker.dto.response;

import com.goaltracker.model.enums.NotificationType;
import java.time.Instant;

public class NotificationResponse {

    private Long id;
    private NotificationType type;
    private String title;
    private String message;
    private boolean read;
    private String metadata;
    private Instant createdAt;

    public NotificationResponse() {}

    public NotificationResponse(Long id, NotificationType type, String title, String message,
                                 boolean read, String metadata, Instant createdAt) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.message = message;
        this.read = read;
        this.metadata = metadata;
        this.createdAt = createdAt;
    }

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    /**
     * Returns a human-readable formatted date string.
     */
    public String getFormattedTime() {
        if (createdAt == null) return "";
        java.time.ZonedDateTime zdt = createdAt.atZone(java.time.ZoneId.of("Europe/Istanbul"));
        return zdt.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));
    }

    /**
     * Returns a Bootstrap icon class based on the notification type.
     */
    public String getIconClass() {
        if (type == null) return "bi-bell";
        return switch (type) {
            case DAILY_REMINDER -> "bi-clock";
            case STREAK_DANGER -> "bi-exclamation-triangle";
            case STREAK_LOST -> "bi-emoji-frown";
            case BADGE_EARNED -> "bi-trophy";
            case GOAL_COMPLETED -> "bi-check-circle";
            case WEEKLY_SUMMARY -> "bi-bar-chart";
            case FRIEND_ACTIVITY -> "bi-people";
        };
    }

    /**
     * Returns a Bootstrap color class based on the notification type.
     */
    public String getColorClass() {
        if (type == null) return "text-primary";
        return switch (type) {
            case DAILY_REMINDER -> "text-info";
            case STREAK_DANGER -> "text-warning";
            case STREAK_LOST -> "text-danger";
            case BADGE_EARNED -> "text-success";
            case GOAL_COMPLETED -> "text-success";
            case WEEKLY_SUMMARY -> "text-primary";
            case FRIEND_ACTIVITY -> "text-secondary";
        };
    }
}


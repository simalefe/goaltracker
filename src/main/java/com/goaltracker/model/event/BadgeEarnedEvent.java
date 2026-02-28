package com.goaltracker.model.event;

import org.springframework.context.ApplicationEvent;

public class BadgeEarnedEvent extends ApplicationEvent {

    private final Long userId;
    private final Long badgeId;
    private final String badgeCode;

    public BadgeEarnedEvent(Object source, Long userId, Long badgeId, String badgeCode) {
        super(source);
        this.userId = userId;
        this.badgeId = badgeId;
        this.badgeCode = badgeCode;
    }

    public Long getUserId() { return userId; }
    public Long getBadgeId() { return badgeId; }
    public String getBadgeCode() { return badgeCode; }
}


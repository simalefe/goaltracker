package com.goaltracker.model.event;

import org.springframework.context.ApplicationEvent;

public class GoalCompletedEvent extends ApplicationEvent {

    private final Long goalId;
    private final Long userId;
    private final String goalTitle;

    public GoalCompletedEvent(Object source, Long goalId, Long userId, String goalTitle) {
        super(source);
        this.goalId = goalId;
        this.userId = userId;
        this.goalTitle = goalTitle;
    }

    public Long getGoalId() { return goalId; }
    public Long getUserId() { return userId; }
    public String getGoalTitle() { return goalTitle; }
}


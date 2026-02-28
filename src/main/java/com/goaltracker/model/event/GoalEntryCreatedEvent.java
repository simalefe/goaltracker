package com.goaltracker.model.event;

import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDate;

public class GoalEntryCreatedEvent extends ApplicationEvent {

    private final Long goalId;
    private final Long userId;
    private final LocalDate entryDate;
    private final BigDecimal actualValue;

    public GoalEntryCreatedEvent(Object source, Long goalId, Long userId,
                                  LocalDate entryDate, BigDecimal actualValue) {
        super(source);
        this.goalId = goalId;
        this.userId = userId;
        this.entryDate = entryDate;
        this.actualValue = actualValue;
    }

    public Long getGoalId() { return goalId; }
    public Long getUserId() { return userId; }
    public LocalDate getEntryDate() { return entryDate; }
    public BigDecimal getActualValue() { return actualValue; }
}


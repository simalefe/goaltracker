package com.goaltracker.model.event;
import org.springframework.context.ApplicationEvent;
import java.time.LocalDate;
public class GoalEntryDeletedEvent extends ApplicationEvent {
    private final Long entryId;
    private final Long goalId;
    private final Long userId;
    private final LocalDate entryDate;
    public GoalEntryDeletedEvent(Object source, Long entryId, Long goalId, Long userId, LocalDate entryDate) {
        super(source);
        this.entryId = entryId;
        this.goalId = goalId;
        this.userId = userId;
        this.entryDate = entryDate;
    }
    public Long getEntryId() { return entryId; }
    public Long getGoalId() { return goalId; }
    public Long getUserId() { return userId; }
    public LocalDate getEntryDate() { return entryDate; }
}

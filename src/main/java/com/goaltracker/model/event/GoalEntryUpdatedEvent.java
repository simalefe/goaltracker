package com.goaltracker.model.event;
import org.springframework.context.ApplicationEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
public class GoalEntryUpdatedEvent extends ApplicationEvent {
    private final Long entryId;
    private final Long goalId;
    private final Long userId;
    private final LocalDate entryDate;
    private final BigDecimal newActualValue;
    public GoalEntryUpdatedEvent(Object source, Long entryId, Long goalId, Long userId,
                                  LocalDate entryDate, BigDecimal newActualValue) {
        super(source);
        this.entryId = entryId;
        this.goalId = goalId;
        this.userId = userId;
        this.entryDate = entryDate;
        this.newActualValue = newActualValue;
    }
    public Long getEntryId() { return entryId; }
    public Long getGoalId() { return goalId; }
    public Long getUserId() { return userId; }
    public LocalDate getEntryDate() { return entryDate; }
    public BigDecimal getNewActualValue() { return newActualValue; }
}

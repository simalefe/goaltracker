package com.goaltracker.mapper;

import com.goaltracker.dto.CreateEntryRequest;
import com.goaltracker.dto.GoalEntryResponse;
import com.goaltracker.model.Goal;
import com.goaltracker.model.GoalEntry;

public class GoalEntryMapper {

    private GoalEntryMapper() {}

    public static GoalEntryResponse toResponse(GoalEntry entry) {
        if (entry == null) return null;
        return new GoalEntryResponse(
                entry.getId(),
                entry.getGoal().getId(),
                entry.getEntryDate(),
                entry.getActualValue(),
                entry.getNote(),
                entry.getCreatedAt()
        );
    }

    public static GoalEntry toEntity(CreateEntryRequest request, Goal goal) {
        if (request == null) return null;
        GoalEntry entry = new GoalEntry();
        entry.setGoal(goal);
        entry.setEntryDate(request.entryDate());
        entry.setActualValue(request.actualValue());
        entry.setNote(request.note());
        return entry;
    }
}


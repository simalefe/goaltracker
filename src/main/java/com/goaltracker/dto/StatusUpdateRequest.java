package com.goaltracker.dto;

import com.goaltracker.model.enums.GoalStatus;
import jakarta.validation.constraints.NotNull;

public class StatusUpdateRequest {
    @NotNull
    private GoalStatus newStatus;

    public GoalStatus getNewStatus() { return newStatus; }
    public void setNewStatus(GoalStatus newStatus) { this.newStatus = newStatus; }
}


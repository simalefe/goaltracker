package com.goaltracker.model.enums;

public enum GoalStatus {
    ACTIVE,
    PAUSED,
    COMPLETED,
    CANCELLED,
    ARCHIVED;

    public static boolean isValidTransition(GoalStatus from, GoalStatus to) {
        return switch (from) {
            case ACTIVE -> to == PAUSED || to == COMPLETED || to == ARCHIVED || to == CANCELLED;
            case PAUSED -> to == ACTIVE || to == ARCHIVED || to == CANCELLED;
            case COMPLETED -> to == ARCHIVED;
            case CANCELLED -> to == ARCHIVED;
            case ARCHIVED -> false;
        };
    }
}

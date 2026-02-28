package com.goaltracker.model.enums;

public enum GoalStatus {
    ACTIVE,
    PAUSED,
    COMPLETED,
    ARCHIVED;

    public static boolean isValidTransition(GoalStatus from, GoalStatus to) {
        return switch (from) {
            case ACTIVE -> to == PAUSED || to == COMPLETED || to == ARCHIVED;
            case PAUSED -> to == ACTIVE || to == ARCHIVED;
            case COMPLETED -> to == ARCHIVED;
            case ARCHIVED -> false;
        };
    }
}


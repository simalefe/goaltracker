package com.goaltracker.exception;

public class GoalAccessDeniedException extends RuntimeException {
    public GoalAccessDeniedException(Long goalId) {
        super("Bu hedefe erişim yetkiniz yok: " + goalId);
    }
}


package com.goaltracker.exception;

public class GoalNotActiveException extends RuntimeException {
    public GoalNotActiveException(String status) {
        super("Bu hedefe kayıt eklenemez. Hedef durumu: " + status);
    }
}


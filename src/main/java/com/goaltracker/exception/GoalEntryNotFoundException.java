package com.goaltracker.exception;

public class GoalEntryNotFoundException extends RuntimeException {
    public GoalEntryNotFoundException(Long id) {
        super("İlerleme kaydı bulunamadı: " + id);
    }
}


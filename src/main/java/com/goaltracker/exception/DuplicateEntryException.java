package com.goaltracker.exception;

public class DuplicateEntryException extends RuntimeException {
    public DuplicateEntryException(String date) {
        super("Bu tarihte zaten bir kayıt mevcut: " + date);
    }
}


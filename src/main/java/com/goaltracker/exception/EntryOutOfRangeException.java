package com.goaltracker.exception;

import java.time.LocalDate;

public class EntryOutOfRangeException extends RuntimeException {
    public EntryOutOfRangeException(LocalDate startDate, LocalDate endDate) {
        super("Kayıt tarihi hedef aralığı dışında. Hedef: " + startDate + " — " + endDate);
    }
}


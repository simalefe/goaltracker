package com.goaltracker.exception;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException() { super("Bu e-posta adresi zaten kayıtlı."); }
}


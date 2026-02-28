package com.goaltracker.exception;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException() { super("Token geçersiz."); }
    public InvalidTokenException(String message) { super(message); }
}


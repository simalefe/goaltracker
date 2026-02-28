package com.goaltracker.exception;

public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException() { super("Token süresi dolmuş."); }
}


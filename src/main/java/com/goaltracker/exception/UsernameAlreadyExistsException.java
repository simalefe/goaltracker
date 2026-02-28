package com.goaltracker.exception;

public class UsernameAlreadyExistsException extends RuntimeException {
    public UsernameAlreadyExistsException() { super("Bu kullanıcı adı zaten alınmış."); }
}


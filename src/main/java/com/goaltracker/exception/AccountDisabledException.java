package com.goaltracker.exception;

public class AccountDisabledException extends RuntimeException {
    public AccountDisabledException() { super("Hesabınız devre dışı bırakılmıştır."); }
}


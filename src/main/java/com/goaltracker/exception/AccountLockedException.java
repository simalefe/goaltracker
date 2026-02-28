package com.goaltracker.exception;

public class AccountLockedException extends RuntimeException {
    public AccountLockedException() {
        super("Hesabınız çok fazla başarısız deneme nedeniyle kilitlendi. 15 dakika sonra tekrar deneyin.");
    }
}


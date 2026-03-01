package com.goaltracker.exception;

public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitExceededException() {
        super("Çok fazla istek gönderdiniz. Lütfen daha sonra tekrar deneyin.");
        this.retryAfterSeconds = 60;
    }

    public RateLimitExceededException(long retryAfterSeconds) {
        super("Çok fazla istek gönderdiniz. Lütfen " + retryAfterSeconds + " saniye sonra tekrar deneyin.");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}


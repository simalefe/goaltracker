package com.goaltracker.exception;

public class FriendshipException extends RuntimeException {
    private final ErrorCode errorCode;

    public FriendshipException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() { return errorCode; }
}


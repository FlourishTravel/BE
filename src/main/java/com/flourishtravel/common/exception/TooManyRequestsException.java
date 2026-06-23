package com.flourishtravel.common.exception;

import lombok.Getter;

@Getter
public class TooManyRequestsException extends RuntimeException {

    private final int retryAfterSeconds;

    public TooManyRequestsException(String message, int retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }
}

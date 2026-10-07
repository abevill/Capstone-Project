package com.fantasyzone.exception;

import org.springframework.http.HttpStatus;

/** A problem with the AI assistant itself (not configured, provider down, rate limited). Carries the HTTP status to return. */
public class LlmException extends RuntimeException {
    private final HttpStatus status;

    public LlmException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }
}

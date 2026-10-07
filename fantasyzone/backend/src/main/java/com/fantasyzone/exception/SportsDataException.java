package com.fantasyzone.exception;

/** The external sports data provider failed or returned something unusable. */
public class SportsDataException extends RuntimeException {
    public SportsDataException(String message, Throwable cause) { super(message, cause); }
}

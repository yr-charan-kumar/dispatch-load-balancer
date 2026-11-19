package com.freightfox.dispatch.exception;

/** Business-level validation failure (e.g. duplicate ids inside one request). Mapped to HTTP 400. */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}

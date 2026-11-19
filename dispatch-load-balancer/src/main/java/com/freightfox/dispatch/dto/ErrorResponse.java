package com.freightfox.dispatch.dto;

import java.time.Instant;
import java.util.List;

/** Uniform error body returned for every 4xx / 5xx. */
public record ErrorResponse(
        String status,
        String message,
        List<FieldIssue> errors,
        Instant timestamp) {

    public record FieldIssue(String field, String message) {
    }

    public static ErrorResponse of(String message) {
        return new ErrorResponse("error", message, List.of(), Instant.now());
    }

    public static ErrorResponse of(String message, List<FieldIssue> errors) {
        return new ErrorResponse("error", message, errors, Instant.now());
    }
}

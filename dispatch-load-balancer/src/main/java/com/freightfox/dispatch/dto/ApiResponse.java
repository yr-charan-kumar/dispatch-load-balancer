package com.freightfox.dispatch.dto;

/** Simple success envelope used by the POST endpoints: {"message": "...", "status": "success"}. */
public record ApiResponse(String message, String status) {

    public static ApiResponse success(String message) {
        return new ApiResponse(message, "success");
    }
}

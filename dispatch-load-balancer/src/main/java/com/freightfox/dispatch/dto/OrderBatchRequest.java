package com.freightfox.dispatch.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OrderBatchRequest(
        @NotEmpty(message = "orders must contain at least one order")
        @Size(max = 10000, message = "orders must contain at most 10000 entries per request")
        List<@NotNull(message = "orders must not contain null entries") @Valid OrderRequest> orders) {
}

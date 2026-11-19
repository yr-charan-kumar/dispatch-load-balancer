package com.freightfox.dispatch.dto;

import java.math.BigDecimal;

/** Order as it appears in the dispatch plan. */
public record OrderDto(
        String orderId,
        double latitude,
        double longitude,
        String address,
        BigDecimal packageWeight,
        String priority) {
}

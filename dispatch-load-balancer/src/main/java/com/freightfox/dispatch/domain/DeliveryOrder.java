package com.freightfox.dispatch.domain;

import java.math.BigDecimal;

/** Pure domain view of an order (independent of JPA / REST). */
public record DeliveryOrder(
        String orderId,
        double latitude,
        double longitude,
        String address,
        BigDecimal packageWeight,
        Priority priority) {
}

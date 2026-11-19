package com.freightfox.dispatch.dto;

import java.math.BigDecimal;

public record DispatchSummary(
        int totalOrders,
        int assignedOrders,
        int unassignedOrders,
        int totalVehicles,
        int vehiclesUsed,
        BigDecimal totalLoad,
        String totalDistance) {
}

package com.freightfox.dispatch.dto;

import java.math.BigDecimal;
import java.util.List;

public record VehiclePlanDto(
        String vehicleId,
        BigDecimal capacity,
        BigDecimal totalLoad,
        BigDecimal remainingCapacity,
        String currentAddress,
        double currentLatitude,
        double currentLongitude,
        String totalDistance,
        double totalDistanceKm,
        List<OrderDto> assignedOrders) {
}

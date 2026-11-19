package com.freightfox.dispatch.domain;

import java.math.BigDecimal;

/** Pure domain view of a vehicle (independent of JPA / REST). */
public record FleetVehicle(
        String vehicleId,
        BigDecimal capacity,
        double currentLatitude,
        double currentLongitude,
        String currentAddress) {
}

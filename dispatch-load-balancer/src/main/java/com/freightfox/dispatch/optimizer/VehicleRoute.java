package com.freightfox.dispatch.optimizer;

import com.freightfox.dispatch.domain.DeliveryOrder;
import com.freightfox.dispatch.domain.FleetVehicle;

import java.math.BigDecimal;
import java.util.List;

/** Final plan for one vehicle: stops in visiting order, total load and distance. */
public record VehicleRoute(
        FleetVehicle vehicle,
        List<DeliveryOrder> stops,
        BigDecimal totalLoad,
        double totalDistanceKm) {
}

package com.freightfox.dispatch.optimizer;

import java.util.List;

public record DispatchResult(List<VehicleRoute> routes, List<UnassignedOrder> unassignedOrders) {
}

package com.freightfox.dispatch.optimizer;

import com.freightfox.dispatch.domain.DeliveryOrder;
import com.freightfox.dispatch.domain.FleetVehicle;

import java.util.List;

/** Strategy for turning orders + fleet into a dispatch plan. Swap implementations freely. */
public interface AssignmentStrategy {

    DispatchResult assign(List<DeliveryOrder> orders, List<FleetVehicle> vehicles);
}

package com.freightfox.dispatch.optimizer;

import com.freightfox.dispatch.domain.DeliveryOrder;

import java.util.List;

/** Strategy that decides the visiting order of the stops assigned to one vehicle. */
public interface RouteSequencer {

    List<DeliveryOrder> sequence(double startLat, double startLon, List<DeliveryOrder> stops);
}

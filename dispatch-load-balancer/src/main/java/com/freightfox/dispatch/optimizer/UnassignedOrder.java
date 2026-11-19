package com.freightfox.dispatch.optimizer;

import com.freightfox.dispatch.domain.DeliveryOrder;

public record UnassignedOrder(DeliveryOrder order, UnassignedReason reason, String message) {
}

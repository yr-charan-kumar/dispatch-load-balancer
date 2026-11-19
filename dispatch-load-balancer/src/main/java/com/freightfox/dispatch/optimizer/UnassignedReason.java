package com.freightfox.dispatch.optimizer;

public enum UnassignedReason {
    /** No vehicles have been registered. */
    NO_VEHICLE_AVAILABLE,
    /** The package is heavier than the largest vehicle in the fleet. */
    EXCEEDS_MAX_VEHICLE_CAPACITY,
    /** Fleet capacity was already used up by higher-priority / earlier orders. */
    INSUFFICIENT_REMAINING_CAPACITY
}

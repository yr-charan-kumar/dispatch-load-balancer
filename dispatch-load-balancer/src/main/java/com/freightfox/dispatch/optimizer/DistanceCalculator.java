package com.freightfox.dispatch.optimizer;

/** Strategy for measuring distance between two coordinates. */
public interface DistanceCalculator {

    /** @return distance in kilometres */
    double distanceKm(double lat1, double lon1, double lat2, double lon2);
}

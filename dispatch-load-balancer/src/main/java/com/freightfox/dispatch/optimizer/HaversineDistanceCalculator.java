package com.freightfox.dispatch.optimizer;

import org.springframework.stereotype.Component;

/** Great-circle distance using the Haversine formula. */
@Component
public class HaversineDistanceCalculator implements DistanceCalculator {

    static final double EARTH_RADIUS_KM = 6371.0;

    @Override
    public double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        if (lat1 == lat2 && lon1 == lon2) {
            return 0.0;
        }
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dPhi = Math.toRadians(lat2 - lat1);
        double dLambda = Math.toRadians(lon2 - lon1);

        double sinDPhi = Math.sin(dPhi / 2);
        double sinDLambda = Math.sin(dLambda / 2);
        double a = sinDPhi * sinDPhi + Math.cos(phi1) * Math.cos(phi2) * sinDLambda * sinDLambda;
        a = Math.min(1.0, a); // guard against floating-point drift above 1
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}

package com.freightfox.dispatch.optimizer;

import com.freightfox.dispatch.domain.DeliveryOrder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Orders stops with the nearest-neighbour heuristic: from the current position always
 * go to the closest unvisited stop. O(k^2) for k stops on one vehicle.
 */
@Component
public class NearestNeighborRouteSequencer implements RouteSequencer {

    private final DistanceCalculator distanceCalculator;

    public NearestNeighborRouteSequencer(DistanceCalculator distanceCalculator) {
        this.distanceCalculator = distanceCalculator;
    }

    @Override
    public List<DeliveryOrder> sequence(double startLat, double startLon, List<DeliveryOrder> stops) {
        int n = stops.size();
        List<DeliveryOrder> result = new ArrayList<>(n);
        boolean[] visited = new boolean[n];
        double curLat = startLat;
        double curLon = startLon;

        for (int step = 0; step < n; step++) {
            int best = -1;
            double bestDist = Double.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                if (visited[i]) {
                    continue;
                }
                DeliveryOrder candidate = stops.get(i);
                double d = distanceCalculator.distanceKm(curLat, curLon, candidate.latitude(), candidate.longitude());
                // strict '<' keeps the earlier (higher priority) stop on ties
                if (d < bestDist) {
                    bestDist = d;
                    best = i;
                }
            }
            visited[best] = true;
            DeliveryOrder next = stops.get(best);
            result.add(next);
            curLat = next.latitude();
            curLon = next.longitude();
        }
        return result;
    }
}

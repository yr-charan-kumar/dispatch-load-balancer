package com.freightfox.dispatch.optimizer;

import com.freightfox.dispatch.domain.DeliveryOrder;
import com.freightfox.dispatch.domain.FleetVehicle;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Greedy, priority-first assignment.
 *
 * <ol>
 *   <li>Sort orders: HIGH before MEDIUM before LOW; inside a priority, heavier first
 *       (first-fit-decreasing packs capacity better), then by orderId for determinism.</li>
 *   <li>For each order pick, among vehicles with enough remaining capacity, the one whose
 *       current position (start, or its last assigned stop) is closest. Ties go to the
 *       vehicle with the most remaining capacity, which balances the load.</li>
 *   <li>Orders that fit nowhere are reported as unassigned with a reason.</li>
 *   <li>Each vehicle's stops are then sequenced with a {@link RouteSequencer} and the
 *       total distance is measured along that route (start -> stop 1 -> ... -> stop n).</li>
 * </ol>
 *
 * Complexity: O(O*V) for assignment + O(k^2) per vehicle for sequencing.
 */
@Component
public class PriorityGreedyAssignmentStrategy implements AssignmentStrategy {

    private static final double EPS = 1e-9;

    private static final Comparator<DeliveryOrder> ORDER_PRECEDENCE =
            Comparator.<DeliveryOrder>comparingInt(o -> o.priority().rank())
                    .thenComparing(DeliveryOrder::packageWeight, Comparator.reverseOrder())
                    .thenComparing(DeliveryOrder::orderId);

    private final DistanceCalculator distanceCalculator;
    private final RouteSequencer routeSequencer;

    public PriorityGreedyAssignmentStrategy(DistanceCalculator distanceCalculator, RouteSequencer routeSequencer) {
        this.distanceCalculator = distanceCalculator;
        this.routeSequencer = routeSequencer;
    }

    @Override
    public DispatchResult assign(List<DeliveryOrder> orders, List<FleetVehicle> vehicles) {
        List<UnassignedOrder> unassigned = new ArrayList<>();
        List<DeliveryOrder> sortedOrders = new ArrayList<>(orders);
        sortedOrders.sort(ORDER_PRECEDENCE);

        if (vehicles.isEmpty()) {
            sortedOrders.forEach(o -> unassigned.add(new UnassignedOrder(o,
                    UnassignedReason.NO_VEHICLE_AVAILABLE, "No vehicles are available.")));
            return new DispatchResult(List.of(), unassigned);
        }

        List<WorkingRoute> working = vehicles.stream()
                .sorted(Comparator.comparing(FleetVehicle::vehicleId))
                .map(WorkingRoute::new)
                .toList();
        BigDecimal maxCapacity = vehicles.stream()
                .map(FleetVehicle::capacity)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);

        for (DeliveryOrder order : sortedOrders) {
            if (order.packageWeight().compareTo(maxCapacity) > 0) {
                unassigned.add(new UnassignedOrder(order, UnassignedReason.EXCEEDS_MAX_VEHICLE_CAPACITY,
                        "Package weight " + order.packageWeight().toPlainString()
                                + " exceeds the largest vehicle capacity " + maxCapacity.toPlainString() + "."));
                continue;
            }
            WorkingRoute best = pickBestVehicle(order, working);
            if (best == null) {
                unassigned.add(new UnassignedOrder(order, UnassignedReason.INSUFFICIENT_REMAINING_CAPACITY,
                        "No vehicle has enough remaining capacity for this order."));
            } else {
                best.add(order);
            }
        }

        List<VehicleRoute> routes = working.stream().map(this::finalizeRoute).toList();
        return new DispatchResult(routes, unassigned);
    }

    private WorkingRoute pickBestVehicle(DeliveryOrder order, List<WorkingRoute> candidates) {
        WorkingRoute best = null;
        double bestDist = Double.MAX_VALUE;
        for (WorkingRoute route : candidates) {
            if (route.remaining.compareTo(order.packageWeight()) < 0) {
                continue;
            }
            double d = distanceCalculator.distanceKm(route.curLat, route.curLon, order.latitude(), order.longitude());
            boolean closer = d < bestDist - EPS;
            boolean tie = Math.abs(d - bestDist) <= EPS;
            if (best == null || closer || (tie && route.remaining.compareTo(best.remaining) > 0)) {
                best = route;
                bestDist = d;
            }
        }
        return best;
    }

    private VehicleRoute finalizeRoute(WorkingRoute w) {
        FleetVehicle v = w.vehicle;
        List<DeliveryOrder> sequenced = routeSequencer.sequence(v.currentLatitude(), v.currentLongitude(), w.stops);
        double distance = 0;
        double lat = v.currentLatitude();
        double lon = v.currentLongitude();
        for (DeliveryOrder stop : sequenced) {
            distance += distanceCalculator.distanceKm(lat, lon, stop.latitude(), stop.longitude());
            lat = stop.latitude();
            lon = stop.longitude();
        }
        return new VehicleRoute(v, sequenced, w.load, distance);
    }

    /** Mutable scratch state used only while assigning. */
    private static final class WorkingRoute {
        final FleetVehicle vehicle;
        final List<DeliveryOrder> stops = new ArrayList<>();
        BigDecimal remaining;
        BigDecimal load = BigDecimal.ZERO;
        double curLat;
        double curLon;

        WorkingRoute(FleetVehicle vehicle) {
            this.vehicle = vehicle;
            this.remaining = vehicle.capacity();
            this.curLat = vehicle.currentLatitude();
            this.curLon = vehicle.currentLongitude();
        }

        void add(DeliveryOrder order) {
            stops.add(order);
            remaining = remaining.subtract(order.packageWeight());
            load = load.add(order.packageWeight());
            curLat = order.latitude();
            curLon = order.longitude();
        }
    }
}

package com.freightfox.dispatch.optimizer;

import com.freightfox.dispatch.domain.DeliveryOrder;
import com.freightfox.dispatch.domain.FleetVehicle;
import com.freightfox.dispatch.domain.Priority;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriorityGreedyAssignmentStrategyTest {

    private static final double[] CP = {28.6139, 77.2090};
    private static final double[] KB = {28.7041, 77.1025};
    private static final double[] NOIDA = {28.5355, 77.3910};
    private static final double[] GURGAON = {28.4595, 77.0266};

    private final HaversineDistanceCalculator distance = new HaversineDistanceCalculator();
    private final PriorityGreedyAssignmentStrategy strategy =
            new PriorityGreedyAssignmentStrategy(distance, new NearestNeighborRouteSequencer(distance));

    private static DeliveryOrder order(String id, double[] at, int weight, Priority p) {
        return new DeliveryOrder(id, at[0], at[1], "addr-" + id, BigDecimal.valueOf(weight), p);
    }

    private static FleetVehicle vehicle(String id, int capacity, double[] at) {
        return new FleetVehicle(id, BigDecimal.valueOf(capacity), at[0], at[1], "depot-" + id);
    }

    private static Set<String> assignedIds(DispatchResult r) {
        return r.routes().stream().flatMap(v -> v.stops().stream()).map(DeliveryOrder::orderId)
                .collect(Collectors.toSet());
    }

    private static void assertInvariants(DispatchResult r, int totalOrders) {
        int assigned = 0;
        for (VehicleRoute route : r.routes()) {
            assertTrue(route.totalLoad().compareTo(route.vehicle().capacity()) <= 0,
                    route.vehicle().vehicleId() + " is over capacity");
            BigDecimal sum = route.stops().stream().map(DeliveryOrder::packageWeight)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertEquals(0, sum.compareTo(route.totalLoad()), "totalLoad must equal sum of stops");
            assigned += route.stops().size();
        }
        assertEquals(totalOrders, assigned + r.unassignedOrders().size(), "every order is assigned or reported");
    }

    @Test
    void highPriorityIsAssignedBeforeLowWhenCapacityIsScarce() {
        List<DeliveryOrder> orders = List.of(order("LOW1", CP, 30, Priority.LOW), order("HIGH1", CP, 30, Priority.HIGH));
        DispatchResult r = strategy.assign(orders, List.of(vehicle("V1", 30, CP)));

        assertEquals(Set.of("HIGH1"), assignedIds(r));
        assertEquals(1, r.unassignedOrders().size());
        assertEquals("LOW1", r.unassignedOrders().get(0).order().orderId());
        assertEquals(UnassignedReason.INSUFFICIENT_REMAINING_CAPACITY, r.unassignedOrders().get(0).reason());
    }

    @Test
    void mediumBeatsLow() {
        List<DeliveryOrder> orders = List.of(order("L", CP, 10, Priority.LOW), order("M", CP, 10, Priority.MEDIUM));
        DispatchResult r = strategy.assign(orders, List.of(vehicle("V1", 10, CP)));
        assertEquals(Set.of("M"), assignedIds(r));
    }

    @Test
    void neverExceedsCapacityAndAcceptsExactFit() {
        List<DeliveryOrder> orders = List.of(order("A", CP, 40, Priority.HIGH), order("B", CP, 60, Priority.HIGH),
                order("C", CP, 1, Priority.HIGH));
        DispatchResult r = strategy.assign(orders, List.of(vehicle("V1", 100, CP)));

        assertEquals(Set.of("A", "B"), assignedIds(r));       // 40 + 60 == capacity exactly
        assertEquals(0, r.routes().get(0).totalLoad().compareTo(BigDecimal.valueOf(100)));
        assertInvariants(r, 3);
    }

    @Test
    void orderHeavierThanEveryVehicleIsReportedWithSpecificReason() {
        DispatchResult r = strategy.assign(List.of(order("BIG", CP, 500, Priority.HIGH)),
                List.of(vehicle("V1", 100, CP), vehicle("V2", 150, KB)));

        assertEquals(1, r.unassignedOrders().size());
        assertEquals(UnassignedReason.EXCEEDS_MAX_VEHICLE_CAPACITY, r.unassignedOrders().get(0).reason());
        assertTrue(assignedIds(r).isEmpty());
    }

    @Test
    void noVehiclesMeansEverythingUnassigned() {
        DispatchResult r = strategy.assign(List.of(order("A", CP, 5, Priority.HIGH), order("B", KB, 5, Priority.LOW)),
                List.of());

        assertTrue(r.routes().isEmpty());
        assertEquals(2, r.unassignedOrders().size());
        assertTrue(r.unassignedOrders().stream().allMatch(u -> u.reason() == UnassignedReason.NO_VEHICLE_AVAILABLE));
    }

    @Test
    void noOrdersGivesIdleVehiclesWithZeroLoadAndDistance() {
        DispatchResult r = strategy.assign(List.of(), List.of(vehicle("V1", 100, CP)));

        assertEquals(1, r.routes().size());
        assertTrue(r.routes().get(0).stops().isEmpty());
        assertEquals(0, r.routes().get(0).totalLoad().signum());
        assertEquals(0.0, r.routes().get(0).totalDistanceKm());
        assertTrue(r.unassignedOrders().isEmpty());
    }

    @Test
    void assignsOrderToTheNearestVehicle() {
        double[] bangalore = {12.9716, 77.5946};
        DispatchResult r = strategy.assign(List.of(order("DEL", CP, 10, Priority.HIGH)),
                List.of(vehicle("BLR", 100, bangalore), vehicle("DELHI", 100, KB)));

        VehicleRoute delhi = r.routes().stream().filter(v -> v.vehicle().vehicleId().equals("DELHI")).findFirst().orElseThrow();
        assertEquals(1, delhi.stops().size());
    }

    @Test
    void tiesAreBrokenByRemainingCapacitySoLoadIsBalanced() {
        // both vehicles start at the same spot; the bigger one should take the first order
        DispatchResult r = strategy.assign(List.of(order("A", KB, 10, Priority.HIGH)),
                List.of(vehicle("SMALL", 50, KB), vehicle("LARGE", 200, KB)));

        VehicleRoute large = r.routes().stream().filter(v -> v.vehicle().vehicleId().equals("LARGE")).findFirst().orElseThrow();
        assertEquals(1, large.stops().size());
    }

    @Test
    void totalDistanceIsMeasuredAlongTheRouteFromTheVehicleStart() {
        DispatchResult r = strategy.assign(List.of(order("A", CP, 10, Priority.HIGH)), List.of(vehicle("V1", 100, KB)));

        double expected = distance.distanceKm(KB[0], KB[1], CP[0], CP[1]);
        assertEquals(expected, r.routes().get(0).totalDistanceKm(), 1e-9);
    }

    @Test
    void vehicleAlreadyAtTheDropHasZeroDistance() {
        DispatchResult r = strategy.assign(List.of(order("A", NOIDA, 10, Priority.HIGH)), List.of(vehicle("V1", 100, NOIDA)));
        assertEquals(0.0, r.routes().get(0).totalDistanceKm(), 1e-9);
    }

    @Test
    void resultIsDeterministic() {
        List<DeliveryOrder> orders = sampleOrders();
        List<FleetVehicle> fleet = sampleFleet();
        DispatchResult first = strategy.assign(orders, fleet);
        DispatchResult second = strategy.assign(orders, fleet);
        assertEquals(first, second);
    }

    @Test
    void officialSampleDataset() {
        DispatchResult r = strategy.assign(sampleOrders(), sampleFleet());

        assertInvariants(r, 30);
        // total weight 645 > total capacity 500, so some orders must be left out ...
        assertTrue(r.unassignedOrders().size() >= 5);
        // ... but never a HIGH or MEDIUM one: they only weigh 420 together
        assertTrue(r.unassignedOrders().stream().allMatch(u -> u.order().priority() == Priority.LOW),
                "only LOW priority orders may be unassigned");
    }

    @Test
    void handlesLargeDatasetQuickly() {
        Random rnd = new Random(42);
        List<DeliveryOrder> orders = new ArrayList<>();
        for (int i = 0; i < 20_000; i++) {
            orders.add(new DeliveryOrder("O" + i, 12 + rnd.nextDouble() * 16, 72 + rnd.nextDouble() * 10, "addr",
                    BigDecimal.valueOf(1 + rnd.nextInt(30)), Priority.values()[rnd.nextInt(3)]));
        }
        List<FleetVehicle> fleet = new ArrayList<>();
        for (int i = 0; i < 300; i++) {
            fleet.add(new FleetVehicle("V" + i, BigDecimal.valueOf(500), 12 + rnd.nextDouble() * 16,
                    72 + rnd.nextDouble() * 10, "depot"));
        }

        long start = System.nanoTime();
        DispatchResult r = strategy.assign(orders, fleet);
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

        assertInvariants(r, 20_000);
        assertTrue(elapsed.compareTo(Duration.ofSeconds(10)) < 0, "took too long: " + elapsed);
    }

    // ---- the 30 orders / 5 vehicles from the assignment sheet ----

    private static List<FleetVehicle> sampleFleet() {
        return List.of(vehicle("VEH001", 100, KB), vehicle("VEH002", 80, NOIDA), vehicle("VEH003", 120, GURGAON),
                vehicle("VEH004", 90, CP), vehicle("VEH005", 110, KB));
    }

    private static List<DeliveryOrder> sampleOrders() {
        Object[][] rows = {
                {1, CP, 15, Priority.HIGH}, {2, CP, 10, Priority.MEDIUM}, {3, KB, 20, Priority.LOW},
                {4, NOIDA, 25, Priority.HIGH}, {5, GURGAON, 30, Priority.MEDIUM}, {6, CP, 40, Priority.LOW},
                {7, KB, 20, Priority.HIGH}, {8, NOIDA, 25, Priority.HIGH}, {9, GURGAON, 15, Priority.MEDIUM},
                {10, CP, 30, Priority.LOW}, {11, KB, 20, Priority.HIGH}, {12, NOIDA, 10, Priority.MEDIUM},
                {13, GURGAON, 25, Priority.HIGH}, {14, CP, 15, Priority.LOW}, {15, KB, 20, Priority.HIGH},
                {16, NOIDA, 30, Priority.LOW}, {17, GURGAON, 15, Priority.MEDIUM}, {18, CP, 10, Priority.HIGH},
                {19, KB, 20, Priority.LOW}, {20, NOIDA, 30, Priority.HIGH}, {21, CP, 25, Priority.MEDIUM},
                {22, KB, 20, Priority.LOW}, {23, NOIDA, 30, Priority.HIGH}, {24, GURGAON, 15, Priority.LOW},
                {25, CP, 20, Priority.MEDIUM}, {26, KB, 25, Priority.HIGH}, {27, NOIDA, 20, Priority.MEDIUM},
                {28, GURGAON, 15, Priority.LOW}, {29, CP, 30, Priority.HIGH}, {30, KB, 20, Priority.LOW}};
        List<DeliveryOrder> orders = new ArrayList<>();
        for (Object[] row : rows) {
            orders.add(order(String.format("ORD%03d", (Integer) row[0]), (double[]) row[1], (Integer) row[2],
                    (Priority) row[3]));
        }
        return orders;
    }
}

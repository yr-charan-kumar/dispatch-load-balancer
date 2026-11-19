package com.freightfox.dispatch.optimizer;

import com.freightfox.dispatch.domain.DeliveryOrder;
import com.freightfox.dispatch.domain.Priority;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NearestNeighborRouteSequencerTest {

    private final NearestNeighborRouteSequencer sequencer =
            new NearestNeighborRouteSequencer(new HaversineDistanceCalculator());

    private static DeliveryOrder order(String id, double lat, double lon) {
        return new DeliveryOrder(id, lat, lon, "addr", BigDecimal.TEN, Priority.MEDIUM);
    }

    @Test
    void emptyListGivesEmptyRoute() {
        assertTrue(sequencer.sequence(0, 0, List.of()).isEmpty());
    }

    @Test
    void visitsClosestStopFirstAlongALine() {
        // all on the equator; start at lon 0
        List<DeliveryOrder> stops = List.of(order("FAR", 0, 3), order("NEAR", 0, 1), order("MID", 0, 2));
        List<String> ids = sequencer.sequence(0, 0, stops).stream().map(DeliveryOrder::orderId).toList();
        assertEquals(List.of("NEAR", "MID", "FAR"), ids);
    }

    @Test
    void keepsAllStops() {
        List<DeliveryOrder> stops = List.of(order("A", 1, 1), order("B", 1, 1), order("C", 2, 2));
        assertEquals(3, sequencer.sequence(0, 0, stops).size());
    }
}

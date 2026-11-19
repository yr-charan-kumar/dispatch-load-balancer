package com.freightfox.dispatch.repository;

import com.freightfox.dispatch.dto.OrderRequest;
import com.freightfox.dispatch.dto.VehicleRequest;
import com.freightfox.dispatch.entity.OrderEntity;
import com.freightfox.dispatch.exception.InvalidRequestException;
import com.freightfox.dispatch.service.DispatchDataService;
import com.freightfox.dispatch.service.UpsertResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Import(DispatchDataService.class)
class DispatchDataServiceUpsertTest {

    @Autowired
    private DispatchDataService service;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private VehicleRepository vehicleRepository;

    private static OrderRequest order(String id, String address, int weight, String priority) {
        return new OrderRequest(id, 12.97, 77.59, address, BigDecimal.valueOf(weight), priority);
    }

    private static VehicleRequest vehicle(String id, int capacity) {
        return new VehicleRequest(id, BigDecimal.valueOf(capacity), 12.97, 77.64, "Indiranagar, Bangalore");
    }

    @Test
    void insertsNewOrders() {
        UpsertResult result = service.saveOrders(List.of(order("A", "MG Road", 10, "HIGH"), order("B", "Anna Salai", 5, "LOW")));

        assertEquals(2, result.inserted());
        assertEquals(0, result.updated());
        assertEquals(2, orderRepository.count());
    }

    @Test
    void postingTheSameOrderIdTwiceUpdatesInsteadOfDuplicating() {
        service.saveOrders(List.of(order("A", "MG Road", 10, "HIGH")));
        orderRepository.flush();

        UpsertResult result = service.saveOrders(List.of(order("A", "New Address", 25, "LOW"), order("B", "Other", 5, "MEDIUM")));
        orderRepository.flush();

        assertEquals(1, result.inserted());
        assertEquals(1, result.updated());
        assertEquals(2, orderRepository.count());
        OrderEntity a = orderRepository.findById("A").orElseThrow();
        assertEquals("New Address", a.getAddress());
        assertEquals(0, BigDecimal.valueOf(25).compareTo(a.getPackageWeight()));
        assertEquals("LOW", a.getPriority().name());
    }

    @Test
    void priorityIsCaseInsensitive() {
        service.saveOrders(List.of(order("A", "MG Road", 10, "high")));
        assertEquals("HIGH", orderRepository.findById("A").orElseThrow().getPriority().name());
    }

    @Test
    void duplicateIdsInsideOneRequestAreRejected() {
        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> service.saveOrders(List.of(order("A", "x", 1, "HIGH"), order("A", "y", 2, "LOW"))));
        assertEquals("Duplicate orderId values in request: [A]", ex.getMessage());
        assertEquals(0, orderRepository.count());
    }

    @Test
    void vehiclesAreUpsertedToo() {
        service.saveVehicles(List.of(vehicle("V1", 100)));
        vehicleRepository.flush();
        UpsertResult result = service.saveVehicles(List.of(vehicle("V1", 250), vehicle("V2", 80)));
        vehicleRepository.flush();

        assertEquals(1, result.inserted());
        assertEquals(1, result.updated());
        assertEquals(2, vehicleRepository.count());
        assertEquals(0, BigDecimal.valueOf(250).compareTo(vehicleRepository.findById("V1").orElseThrow().getCapacity()));
    }

    @Test
    void clearAllRemovesEverything() {
        service.saveOrders(List.of(order("A", "x", 1, "HIGH")));
        service.saveVehicles(List.of(vehicle("V1", 100)));
        service.clearAll();
        assertEquals(0, orderRepository.count());
        assertEquals(0, vehicleRepository.count());
    }
}

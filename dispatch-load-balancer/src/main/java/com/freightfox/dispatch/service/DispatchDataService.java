package com.freightfox.dispatch.service;

import com.freightfox.dispatch.domain.Priority;
import com.freightfox.dispatch.dto.OrderRequest;
import com.freightfox.dispatch.dto.VehicleRequest;
import com.freightfox.dispatch.entity.OrderEntity;
import com.freightfox.dispatch.entity.VehicleEntity;
import com.freightfox.dispatch.exception.InvalidRequestException;
import com.freightfox.dispatch.repository.OrderRepository;
import com.freightfox.dispatch.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
public class DispatchDataService {

    private final OrderRepository orderRepository;
    private final VehicleRepository vehicleRepository;

    public DispatchDataService(OrderRepository orderRepository, VehicleRepository vehicleRepository) {
        this.orderRepository = orderRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional
    public UpsertResult saveOrders(List<OrderRequest> requests) {
        List<String> ids = requests.stream().map(r -> r.orderId().trim()).toList();
        rejectDuplicates(ids, "orderId");

        Map<String, OrderEntity> existing = orderRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(OrderEntity::getOrderId, Function.identity()));

        List<OrderEntity> toSave = new ArrayList<>(requests.size());
        int updated = 0;
        for (OrderRequest r : requests) {
            String id = r.orderId().trim();
            OrderEntity entity = existing.get(id);
            if (entity == null) {
                entity = new OrderEntity();
                entity.setOrderId(id);
            } else {
                updated++;
            }
            entity.update(r.latitude(), r.longitude(), r.address().trim(), r.packageWeight(),
                    Priority.valueOf(r.priority().trim().toUpperCase(Locale.ROOT)));
            toSave.add(entity);
        }
        orderRepository.saveAll(toSave);
        return new UpsertResult(toSave.size() - updated, updated);
    }

    @Transactional
    public UpsertResult saveVehicles(List<VehicleRequest> requests) {
        List<String> ids = requests.stream().map(r -> r.vehicleId().trim()).toList();
        rejectDuplicates(ids, "vehicleId");

        Map<String, VehicleEntity> existing = vehicleRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(VehicleEntity::getVehicleId, Function.identity()));

        List<VehicleEntity> toSave = new ArrayList<>(requests.size());
        int updated = 0;
        for (VehicleRequest r : requests) {
            String id = r.vehicleId().trim();
            VehicleEntity entity = existing.get(id);
            if (entity == null) {
                entity = new VehicleEntity();
                entity.setVehicleId(id);
            } else {
                updated++;
            }
            entity.update(r.capacity(), r.currentLatitude(), r.currentLongitude(), r.currentAddress().trim());
            toSave.add(entity);
        }
        vehicleRepository.saveAll(toSave);
        return new UpsertResult(toSave.size() - updated, updated);
    }

    @Transactional
    public void clearAll() {
        orderRepository.deleteAllInBatch();
        vehicleRepository.deleteAllInBatch();
    }

    private void rejectDuplicates(List<String> ids, String fieldName) {
        Set<String> seen = new HashSet<>();
        Set<String> duplicates = new LinkedHashSet<>();
        for (String id : ids) {
            if (!seen.add(id)) {
                duplicates.add(id);
            }
        }
        if (!duplicates.isEmpty()) {
            throw new InvalidRequestException("Duplicate " + fieldName + " values in request: " + duplicates);
        }
    }
}

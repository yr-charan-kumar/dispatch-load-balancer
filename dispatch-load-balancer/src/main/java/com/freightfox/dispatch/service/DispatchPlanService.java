package com.freightfox.dispatch.service;

import com.freightfox.dispatch.domain.DeliveryOrder;
import com.freightfox.dispatch.domain.FleetVehicle;
import com.freightfox.dispatch.dto.DispatchPlanResponse;
import com.freightfox.dispatch.mapper.DispatchMapper;
import com.freightfox.dispatch.optimizer.AssignmentStrategy;
import com.freightfox.dispatch.optimizer.DispatchResult;
import com.freightfox.dispatch.repository.OrderRepository;
import com.freightfox.dispatch.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class DispatchPlanService {

    private final OrderRepository orderRepository;
    private final VehicleRepository vehicleRepository;
    private final AssignmentStrategy assignmentStrategy;
    private final DispatchMapper mapper;

    public DispatchPlanService(OrderRepository orderRepository,
                               VehicleRepository vehicleRepository,
                               AssignmentStrategy assignmentStrategy,
                               DispatchMapper mapper) {
        this.orderRepository = orderRepository;
        this.vehicleRepository = vehicleRepository;
        this.assignmentStrategy = assignmentStrategy;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public DispatchPlanResponse generatePlan() {
        List<DeliveryOrder> orders = orderRepository.findAll().stream().map(mapper::toDomain).toList();
        List<FleetVehicle> vehicles = vehicleRepository.findAll().stream().map(mapper::toDomain).toList();
        DispatchResult result = assignmentStrategy.assign(orders, vehicles);
        return mapper.toResponse(result, orders.size(), vehicles.size());
    }
}

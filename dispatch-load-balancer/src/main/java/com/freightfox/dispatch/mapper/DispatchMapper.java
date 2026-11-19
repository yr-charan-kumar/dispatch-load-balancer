package com.freightfox.dispatch.mapper;

import com.freightfox.dispatch.domain.DeliveryOrder;
import com.freightfox.dispatch.domain.FleetVehicle;
import com.freightfox.dispatch.dto.DispatchPlanResponse;
import com.freightfox.dispatch.dto.DispatchSummary;
import com.freightfox.dispatch.dto.OrderDto;
import com.freightfox.dispatch.dto.UnassignedOrderDto;
import com.freightfox.dispatch.dto.VehiclePlanDto;
import com.freightfox.dispatch.entity.OrderEntity;
import com.freightfox.dispatch.entity.VehicleEntity;
import com.freightfox.dispatch.optimizer.DispatchResult;
import com.freightfox.dispatch.optimizer.UnassignedOrder;
import com.freightfox.dispatch.optimizer.VehicleRoute;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

/** Converts between persistence entities, domain records and API DTOs. */
@Component
public class DispatchMapper {

    public DeliveryOrder toDomain(OrderEntity e) {
        return new DeliveryOrder(e.getOrderId(), e.getLatitude(), e.getLongitude(), e.getAddress(),
                e.getPackageWeight(), e.getPriority());
    }

    public FleetVehicle toDomain(VehicleEntity e) {
        return new FleetVehicle(e.getVehicleId(), e.getCapacity(), e.getCurrentLatitude(),
                e.getCurrentLongitude(), e.getCurrentAddress());
    }

    public OrderDto toDto(DeliveryOrder o) {
        return new OrderDto(o.orderId(), o.latitude(), o.longitude(), o.address(),
                normalize(o.packageWeight()), o.priority().name());
    }

    public DispatchPlanResponse toResponse(DispatchResult result, int totalOrders, int totalVehicles) {
        List<VehiclePlanDto> plan = result.routes().stream().map(this::toPlanDto).toList();
        List<UnassignedOrderDto> unassigned = result.unassignedOrders().stream().map(this::toDto).toList();

        int assigned = result.routes().stream().mapToInt(r -> r.stops().size()).sum();
        int used = (int) result.routes().stream().filter(r -> !r.stops().isEmpty()).count();
        BigDecimal load = result.routes().stream().map(VehicleRoute::totalLoad).reduce(BigDecimal.ZERO, BigDecimal::add);
        double km = result.routes().stream().mapToDouble(VehicleRoute::totalDistanceKm).sum();

        DispatchSummary summary = new DispatchSummary(totalOrders, assigned, unassigned.size(),
                totalVehicles, used, normalize(load), formatKm(km));
        return new DispatchPlanResponse(plan, unassigned, summary);
    }

    private VehiclePlanDto toPlanDto(VehicleRoute r) {
        FleetVehicle v = r.vehicle();
        return new VehiclePlanDto(
                v.vehicleId(),
                normalize(v.capacity()),
                normalize(r.totalLoad()),
                normalize(v.capacity().subtract(r.totalLoad())),
                v.currentAddress(),
                v.currentLatitude(),
                v.currentLongitude(),
                formatKm(r.totalDistanceKm()),
                round2(r.totalDistanceKm()),
                r.stops().stream().map(this::toDto).toList());
    }

    private UnassignedOrderDto toDto(UnassignedOrder u) {
        return new UnassignedOrderDto(toDto(u.order()), u.reason().name(), u.message());
    }

    /** 100.000 -> 100, 12.500 -> 12.5 (so JSON shows 100, not 100.000 or 1E+2). */
    static BigDecimal normalize(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }

    static double round2(double km) {
        return BigDecimal.valueOf(km).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    static String formatKm(double km) {
        return String.format(Locale.ROOT, "%.2f km", km);
    }
}

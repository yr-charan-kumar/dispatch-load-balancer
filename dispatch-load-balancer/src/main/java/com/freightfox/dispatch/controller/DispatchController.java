package com.freightfox.dispatch.controller;

import com.freightfox.dispatch.dto.ApiResponse;
import com.freightfox.dispatch.dto.DispatchPlanResponse;
import com.freightfox.dispatch.dto.ErrorResponse;
import com.freightfox.dispatch.dto.OrderBatchRequest;
import com.freightfox.dispatch.dto.VehicleBatchRequest;
import com.freightfox.dispatch.service.DispatchDataService;
import com.freightfox.dispatch.service.DispatchPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/dispatch", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Dispatch", description = "Submit orders and fleet, then fetch the optimized dispatch plan")
public class DispatchController {

    private final DispatchDataService dataService;
    private final DispatchPlanService planService;

    public DispatchController(DispatchDataService dataService, DispatchPlanService planService) {
        this.dataService = dataService;
        this.planService = planService;
    }

    @Operation(summary = "Submit delivery orders",
            description = "Upsert: an order with an existing orderId is updated, otherwise created.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Orders accepted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/orders", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse submitOrders(@Valid @RequestBody OrderBatchRequest request) {
        dataService.saveOrders(request.orders());
        return ApiResponse.success("Delivery orders accepted.");
    }

    @Operation(summary = "Submit fleet details",
            description = "Upsert: a vehicle with an existing vehicleId is updated, otherwise created.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Vehicles accepted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/vehicles", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse submitVehicles(@Valid @RequestBody VehicleBatchRequest request) {
        dataService.saveVehicles(request.vehicles());
        return ApiResponse.success("Vehicle details accepted.");
    }

    @Operation(summary = "Get the optimized dispatch plan",
            description = "Computes the plan from the stored orders and vehicles. Orders that cannot be "
                    + "assigned are listed under unassignedOrders with a reason.")
    @GetMapping("/plan")
    public DispatchPlanResponse getPlan() {
        return planService.generatePlan();
    }

    @Operation(summary = "Delete all stored orders and vehicles",
            description = "Convenience endpoint to start a fresh scenario while testing.")
    @DeleteMapping("/data")
    public ApiResponse reset() {
        dataService.clearAll();
        return ApiResponse.success("All orders and vehicles deleted.");
    }
}

package com.freightfox.dispatch.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record VehicleBatchRequest(
        @NotEmpty(message = "vehicles must contain at least one vehicle")
        @Size(max = 10000, message = "vehicles must contain at most 10000 entries per request")
        List<@NotNull(message = "vehicles must not contain null entries") @Valid VehicleRequest> vehicles) {
}

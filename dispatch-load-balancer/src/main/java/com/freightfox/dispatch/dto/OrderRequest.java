package com.freightfox.dispatch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "A delivery order")
public record OrderRequest(
        @Schema(example = "ORD001")
        @NotBlank(message = "orderId is required")
        @Size(max = 64, message = "orderId must be at most 64 characters")
        String orderId,

        @Schema(example = "28.6139")
        @NotNull(message = "latitude is required")
        @DecimalMin(value = "-90.0", message = "latitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "latitude must be between -90 and 90")
        Double latitude,

        @Schema(example = "77.2090")
        @NotNull(message = "longitude is required")
        @DecimalMin(value = "-180.0", message = "longitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "longitude must be between -180 and 180")
        Double longitude,

        @Schema(example = "Connaught Place, Delhi, India")
        @NotBlank(message = "address is required")
        @Size(max = 500, message = "address must be at most 500 characters")
        String address,

        @Schema(example = "15")
        @NotNull(message = "packageWeight is required")
        @Positive(message = "packageWeight must be greater than 0")
        BigDecimal packageWeight,

        @Schema(example = "HIGH", allowableValues = {"HIGH", "MEDIUM", "LOW"})
        @NotBlank(message = "priority is required")
        @Pattern(regexp = "(?i)HIGH|MEDIUM|LOW", message = "priority must be one of HIGH, MEDIUM, LOW")
        String priority) {
}

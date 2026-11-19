package com.freightfox.dispatch.dto;

public record UnassignedOrderDto(OrderDto order, String reason, String message) {
}

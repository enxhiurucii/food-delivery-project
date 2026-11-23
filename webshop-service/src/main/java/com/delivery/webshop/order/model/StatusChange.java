package com.delivery.webshop.order.model;

import java.time.Instant;

import com.delivery.webshop.order.enums.OrderStatus;

public record StatusChange(OrderStatus status, Instant at) {
}

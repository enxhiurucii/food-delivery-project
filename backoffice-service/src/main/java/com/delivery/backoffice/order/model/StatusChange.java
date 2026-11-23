package com.delivery.backoffice.order.model;

import java.time.Instant;

import com.delivery.backoffice.order.enums.OrderStatus;

/** One entry of the order's timeline. */
public record StatusChange(OrderStatus status, Instant at) {
}

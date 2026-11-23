package com.delivery.backoffice.shared.messaging.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Integration event published by the back office (routing key "order.status.changed").
 * {@code sequence} increases by one per status change, so consumers can ignore duplicates and
 * out-of-order deliveries.
 */
public record OrderStatusChangedEvent(
        UUID eventId,
        Instant occurredAt,
        String orderId,
        String restaurantId,
        String status,
        int sequence,
        String paymentStatus,
        Instant changedAt) {
}

package com.delivery.webshop.shared.messaging.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Integration event published by the webshop (routing key "order.placed").
 * Deliberately duplicated in both services instead of a shared library: each service owns its copy
 * of the contract, enums travel as strings, and unknown JSON fields are ignored, so the services
 * can be deployed independently.
 */
public record OrderPlacedEvent(
        UUID eventId,
        Instant occurredAt,
        String orderId,
        String restaurantId,
        String customerId,
        String customerName,
        List<Line> lines,
        long subtotalCents,
        long deliveryFeeCents,
        long totalCents,
        String fulfillmentType,
        Address deliveryAddress,
        String notes,
        String paymentMethod,
        String paymentTiming,
        String paymentStatus) {

    public record Line(String menuItemId, String name, long unitPriceCents, int quantity) {
    }

    public record Address(String street, String city, String postalCode, String phone, String instructions) {
    }
}

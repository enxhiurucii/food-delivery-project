package com.delivery.webshop.catalog.dto;

import java.util.List;

public record PricingResponse(
        String restaurantId, String restaurantName,
        boolean acceptingOrders, boolean pickupEnabled, boolean deliveryEnabled,
        long deliveryFeeCents, long minimumOrderCents, int estimatedPrepMinutes,
        List<PricedLine> lines) {

    public long subtotalCents() {
        return lines.stream().mapToLong(line -> line.unitPriceCents() * line.quantity()).sum();
    }
}

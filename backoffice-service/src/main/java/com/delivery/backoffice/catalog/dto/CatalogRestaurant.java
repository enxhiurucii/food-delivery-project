package com.delivery.backoffice.catalog.dto;

public record CatalogRestaurant(
        String id, String name, String description, String address, String imageUrl,
        boolean acceptingOrders, boolean pickupEnabled, boolean deliveryEnabled,
        long deliveryFeeCents, long minimumOrderCents, int estimatedPrepMinutes) {
}

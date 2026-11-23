package com.delivery.webshop.catalog.dto;

import java.math.BigDecimal;

import com.delivery.webshop.shared.money.Money;

public record RestaurantView(String id, String name, String description, String address, String imageUrl,
                             boolean open, boolean pickupAvailable, boolean deliveryAvailable,
                             BigDecimal deliveryFee, BigDecimal minimumOrder, int estimatedPrepMinutes,
                             String currency) {
    public static RestaurantView from(CatalogRestaurant r) {
        return new RestaurantView(r.id(), r.name(), r.description(), r.address(), r.imageUrl(),
                r.acceptingOrders(), r.pickupEnabled(), r.deliveryEnabled(),
                Money.toDecimal(r.deliveryFeeCents()), Money.toDecimal(r.minimumOrderCents()),
                r.estimatedPrepMinutes(), Money.CURRENCY);
    }
}

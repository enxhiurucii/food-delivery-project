package com.delivery.backoffice.restaurant.dto;

import java.math.BigDecimal;

import com.delivery.backoffice.restaurant.model.Restaurant;
import com.delivery.backoffice.shared.money.Money;

public record RestaurantResponse(
        String id, String name, String description, String address, String phone, String imageUrl,
        String timezone, boolean acceptingOrders, boolean pickupEnabled, boolean deliveryEnabled,
        BigDecimal deliveryFee, BigDecimal minimumOrder, int estimatedPrepMinutes, String currency) {

    public static RestaurantResponse from(Restaurant r) {
        return new RestaurantResponse(r.getId(), r.getName(), r.getDescription(), r.getAddress(), r.getPhone(),
                r.getImageUrl(), r.getTimezone(), r.isAcceptingOrders(), r.isPickupEnabled(),
                r.isDeliveryEnabled(), Money.toDecimal(r.getDeliveryFeeCents()),
                Money.toDecimal(r.getMinimumOrderCents()), r.getEstimatedPrepMinutes(), Money.CURRENCY);
    }
}

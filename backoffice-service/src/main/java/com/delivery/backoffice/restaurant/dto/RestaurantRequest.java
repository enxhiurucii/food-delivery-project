package com.delivery.backoffice.restaurant.dto;

import java.math.BigDecimal;

import com.delivery.backoffice.restaurant.model.RestaurantDetails;
import com.delivery.backoffice.shared.money.Money;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RestaurantRequest(
        @NotBlank String name,
        String description,
        @NotBlank String address,
        String phone,
        String imageUrl,
        @NotBlank String timezone,
        boolean acceptingOrders,
        boolean pickupEnabled,
        boolean deliveryEnabled,
        @NotNull @DecimalMin("0.00") BigDecimal deliveryFee,
        @NotNull @DecimalMin("0.00") BigDecimal minimumOrder,
        @Min(1) int estimatedPrepMinutes) {

    public RestaurantDetails toDetails() {
        return new RestaurantDetails(name, description, address, phone, imageUrl, timezone, acceptingOrders,
                pickupEnabled, deliveryEnabled, Money.toCents(deliveryFee), Money.toCents(minimumOrder),
                estimatedPrepMinutes);
    }
}

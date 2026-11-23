package com.delivery.backoffice.restaurant.model;

import java.time.DateTimeException;
import java.time.ZoneId;

import com.delivery.backoffice.shared.error.BusinessRuleException;

/** Everything a manager can configure about a restaurant. */
public record RestaurantDetails(
        String name,
        String description,
        String address,
        String phone,
        String imageUrl,
        String timezone,
        boolean acceptingOrders,
        boolean pickupEnabled,
        boolean deliveryEnabled,
        long deliveryFeeCents,
        long minimumOrderCents,
        int estimatedPrepMinutes) {

    void validate() {
        if (!pickupEnabled && !deliveryEnabled) {
            throw new BusinessRuleException("NO_FULFILLMENT_TYPE", "Enable at least pickup or delivery");
        }
        try {
            ZoneId.of(timezone);
        } catch (DateTimeException | NullPointerException ex) {
            throw new BusinessRuleException("INVALID_TIMEZONE", "Unknown timezone '" + timezone + "'");
        }
    }
}

package com.delivery.webshop.checkout.dto;

import com.delivery.webshop.order.enums.FulfillmentType;
import com.delivery.webshop.order.model.DeliveryAddress;
import com.delivery.webshop.order.model.Payment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CheckoutRequest(
        @NotNull FulfillmentType fulfillmentType,
        @Valid AddressRequest deliveryAddress,
        @NotNull Payment.Method paymentMethod,
        @NotNull Payment.Timing paymentTiming,
        @Size(max = 500) String notes) {

    public CheckoutCommand toCommand() {
        DeliveryAddress address = deliveryAddress == null ? null : new DeliveryAddress(deliveryAddress.street(),
                deliveryAddress.city(), deliveryAddress.postalCode(), deliveryAddress.phone(),
                deliveryAddress.instructions());
        return new CheckoutCommand(fulfillmentType, address, paymentMethod, paymentTiming, notes);
    }
}

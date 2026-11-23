package com.delivery.webshop.checkout.dto;

import com.delivery.webshop.order.model.DeliveryAddress;
import com.delivery.webshop.order.enums.FulfillmentType;
import com.delivery.webshop.order.model.Payment;

public record CheckoutCommand(
        FulfillmentType fulfillmentType,
        DeliveryAddress deliveryAddress,
        Payment.Method paymentMethod,
        Payment.Timing paymentTiming,
        String notes) {
}

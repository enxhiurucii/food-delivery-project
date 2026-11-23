package com.delivery.webshop.order.model;

import com.delivery.webshop.order.enums.FulfillmentType;
import com.delivery.webshop.shared.error.BusinessRuleException;

/** Which payment options are allowed for which fulfillment type. Pure domain logic, no Spring. */
public final class PaymentPolicy {

    private PaymentPolicy() {
    }

    public static void validate(FulfillmentType fulfillment, Payment.Method method, Payment.Timing timing) {
        if (method == null || timing == null) {
            throw new BusinessRuleException("PAYMENT_REQUIRED", "Choose a payment method");
        }
        boolean allowed = switch (timing) {
            case ONLINE -> method == Payment.Method.CARD;                          // pay now: card only
            case ON_COMPLETION -> fulfillment == FulfillmentType.PICKUP            // pay at the restaurant: cash or card
                    || method == Payment.Method.CASH;                              // delivery: cash to the courier
        };
        if (!allowed) {
            throw new BusinessRuleException("PAYMENT_OPTION_NOT_ALLOWED",
                    method + " / " + timing + " is not available for " + fulfillment);
        }
    }
}

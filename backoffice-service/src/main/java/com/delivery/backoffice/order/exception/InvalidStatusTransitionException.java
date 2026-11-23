package com.delivery.backoffice.order.exception;

import com.delivery.backoffice.order.enums.FulfillmentType;
import com.delivery.backoffice.order.enums.OrderStatus;

public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(String orderId, OrderStatus from, OrderStatus to, FulfillmentType type) {
        super("Order " + orderId + " (" + type + ") cannot move from " + from + " to " + to);
    }
}

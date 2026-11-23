package com.delivery.webshop.order.enums;

import java.util.List;

/**
 * The webshop's copy of the order lifecycle. The back office owns the transitions; the webshop only
 * projects them for the customer and knows the expected "happy path" to draw the timeline.
 */
public enum OrderStatus {
    PLACED,
    ACCEPTED,
    PREPARING,
    READY_FOR_PICKUP,
    OUT_FOR_DELIVERY,
    PICKED_UP,
    DELIVERED,
    REJECTED,
    CANCELLED;

    public static List<OrderStatus> happyPath(FulfillmentType type) {
        return type == FulfillmentType.PICKUP
                ? List.of(PLACED, ACCEPTED, PREPARING, READY_FOR_PICKUP, PICKED_UP)
                : List.of(PLACED, ACCEPTED, PREPARING, OUT_FOR_DELIVERY, DELIVERED);
    }

    public boolean isTerminal() {
        return this == PICKED_UP || this == DELIVERED || this == REJECTED || this == CANCELLED;
    }
}

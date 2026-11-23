package com.delivery.webshop.shared.messaging;

/** Routing keys on the "orders.events" topic exchange. Must match the back-office service. */
public final class RoutingKeys {

    /** Published here when a customer checks out. Consumed by the back office. */
    public static final String ORDER_PLACED = "order.placed";
    /** Published by the back office when staff move an order forward. Consumed here. */
    public static final String ORDER_STATUS_CHANGED = "order.status.changed";

    private RoutingKeys() {
    }
}

package com.delivery.backoffice.shared.messaging;

/** Routing keys on the "orders.events" topic exchange. Must match the webshop service. */
public final class RoutingKeys {

    /** Published by the webshop when a customer checks out. Consumed here. */
    public static final String ORDER_PLACED = "order.placed";
    /** Published here when restaurant staff move an order forward. Consumed by the webshop. */
    public static final String ORDER_STATUS_CHANGED = "order.status.changed";

    private RoutingKeys() {
    }
}

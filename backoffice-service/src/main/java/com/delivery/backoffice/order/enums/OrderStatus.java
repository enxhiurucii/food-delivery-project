package com.delivery.backoffice.order.enums;

import java.util.List;

/**
 * Order lifecycle. The allowed transitions depend on the fulfillment type:
 * <pre>
 * PLACED -> ACCEPTED -> PREPARING -> READY_FOR_PICKUP -> PICKED_UP             (pickup, staff)
 *                                                     -> OUT_FOR_DELIVERY      (delivery, driver)
 *                                                        -> DELIVERED          (delivery, driver)
 * PLACED -> REJECTED                   (restaurant declines, e.g. too busy)
 * PLACED | ACCEPTED -> CANCELLED       (only before the kitchen starts)
 * </pre>
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

    public boolean canTransitionTo(OrderStatus next, FulfillmentType fulfillmentType) {
        return switch (this) {
            case PLACED -> next == ACCEPTED || next == REJECTED || next == CANCELLED;
            case ACCEPTED -> next == PREPARING || next == CANCELLED;
            case PREPARING -> next == READY_FOR_PICKUP;
            case READY_FOR_PICKUP -> fulfillmentType == FulfillmentType.PICKUP
                    ? next == PICKED_UP
                    : next == OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> next == DELIVERED;
            case PICKED_UP, DELIVERED, REJECTED, CANCELLED -> false;
        };
    }

    /**
     * Transitions that restaurant staff can trigger (excludes driver-only transitions such as
     * READY_FOR_PICKUP → OUT_FOR_DELIVERY for DELIVERY orders).
     */
    public List<OrderStatus> staffTransitions(FulfillmentType fulfillmentType) {
        return switch (this) {
            case PLACED -> List.of(ACCEPTED, REJECTED, CANCELLED);
            case ACCEPTED -> List.of(PREPARING, CANCELLED);
            case PREPARING -> List.of(READY_FOR_PICKUP);
            case READY_FOR_PICKUP -> fulfillmentType == FulfillmentType.PICKUP
                    ? List.of(PICKED_UP)
                    : List.of();
            default -> List.of();
        };
    }

    /** No further transitions possible. */
    public boolean isTerminal() {
        return this == PICKED_UP || this == DELIVERED || this == REJECTED || this == CANCELLED;
    }

    /** Successfully handed to the customer: counts as revenue and appears in receipts/reports. */
    public boolean isCompleted() {
        return this == PICKED_UP || this == DELIVERED;
    }
}

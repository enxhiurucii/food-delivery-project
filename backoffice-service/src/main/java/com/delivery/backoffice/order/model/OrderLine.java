package com.delivery.backoffice.order.model;

/**
 * Snapshot of the item at order time. Later menu edits (price/name) must not change past orders or receipts.
 */
public record OrderLine(String menuItemId, String name, long unitPriceCents, int quantity) {

    public long totalCents() {
        return unitPriceCents * quantity;
    }
}

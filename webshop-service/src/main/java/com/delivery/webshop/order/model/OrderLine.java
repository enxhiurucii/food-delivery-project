package com.delivery.webshop.order.model;

public record OrderLine(String menuItemId, String name, long unitPriceCents, int quantity) {

    public long totalCents() {
        return unitPriceCents * quantity;
    }
}

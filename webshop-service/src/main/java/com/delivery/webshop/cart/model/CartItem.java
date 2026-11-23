package com.delivery.webshop.cart.model;

/** Name and price are a display snapshot; checkout re-prices everything against the back office. */
public record CartItem(String menuItemId, String name, long unitPriceCents, int quantity) {

    public long totalCents() {
        return unitPriceCents * quantity;
    }

    CartItem withQuantity(int newQuantity) {
        return new CartItem(menuItemId, name, unitPriceCents, newQuantity);
    }
}

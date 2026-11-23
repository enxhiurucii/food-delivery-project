package com.delivery.backoffice.menu.model;

/** A sellable item, e.g. "Burger menu" for 10.00 EUR. Embedded in {@link Menu}. */
public record MenuItem(
        String id,
        String name,
        String description,
        long priceCents,
        String imageUrl,
        boolean available) {
}

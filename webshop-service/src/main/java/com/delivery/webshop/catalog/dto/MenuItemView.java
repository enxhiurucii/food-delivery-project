package com.delivery.webshop.catalog.dto;

import java.math.BigDecimal;

import com.delivery.webshop.shared.money.Money;

public record MenuItemView(String id, String name, String description, BigDecimal price, String currency,
                           String imageUrl, boolean available) {
    public static MenuItemView from(CatalogItem item) {
        return new MenuItemView(item.id(), item.name(), item.description(), Money.toDecimal(item.priceCents()),
                Money.CURRENCY, item.imageUrl(), item.available());
    }
}

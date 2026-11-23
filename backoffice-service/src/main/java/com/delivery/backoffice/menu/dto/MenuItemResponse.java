package com.delivery.backoffice.menu.dto;

import java.math.BigDecimal;

import com.delivery.backoffice.menu.model.MenuItem;
import com.delivery.backoffice.shared.money.Money;

public record MenuItemResponse(String id, String name, String description, BigDecimal price, String currency,
                               String imageUrl, boolean available) {
    public static MenuItemResponse from(MenuItem item) {
        return new MenuItemResponse(item.id(), item.name(), item.description(), Money.toDecimal(item.priceCents()),
                Money.CURRENCY, item.imageUrl(), item.available());
    }
}

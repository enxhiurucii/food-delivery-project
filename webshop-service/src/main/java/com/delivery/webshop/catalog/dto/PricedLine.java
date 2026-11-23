package com.delivery.webshop.catalog.dto;

public record PricedLine(String menuItemId, String name, long unitPriceCents, int quantity) {
}

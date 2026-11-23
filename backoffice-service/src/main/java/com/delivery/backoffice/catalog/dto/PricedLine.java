package com.delivery.backoffice.catalog.dto;

public record PricedLine(String menuItemId, String name, long unitPriceCents, int quantity) {
}

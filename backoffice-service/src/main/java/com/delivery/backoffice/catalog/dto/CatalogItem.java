package com.delivery.backoffice.catalog.dto;

public record CatalogItem(String id, String name, String description, long priceCents, String imageUrl,
                          boolean available) {
}

package com.delivery.webshop.catalog.dto;

import java.util.List;

public record CatalogMenu(String id, String name, String description, List<CatalogItem> items) {
}

package com.delivery.backoffice.catalog.dto;

import java.util.List;

public record CatalogMenu(String id, String name, String description, List<CatalogItem> items) {
}

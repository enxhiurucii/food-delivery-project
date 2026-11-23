package com.delivery.webshop.catalog.dto;

import java.util.List;

public record MenuView(String id, String name, String description, List<MenuItemView> items) {
    public static MenuView from(CatalogMenu menu) {
        return new MenuView(menu.id(), menu.name(), menu.description(),
                menu.items().stream().map(MenuItemView::from).toList());
    }
}

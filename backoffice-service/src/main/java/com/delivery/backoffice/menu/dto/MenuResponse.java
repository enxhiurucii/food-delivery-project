package com.delivery.backoffice.menu.dto;

import java.util.List;

import com.delivery.backoffice.menu.model.Menu;

public record MenuResponse(String id, String name, String description, int position,
                           List<MenuItemResponse> items) {
    public static MenuResponse from(Menu menu) {
        return new MenuResponse(menu.getId(), menu.getName(), menu.getDescription(), menu.getPosition(),
                menu.getItems().stream().map(MenuItemResponse::from).toList());
    }
}

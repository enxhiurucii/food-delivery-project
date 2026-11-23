package com.delivery.backoffice.menu.service;

import java.util.List;

import com.delivery.backoffice.menu.model.Menu;
import com.delivery.backoffice.menu.model.MenuItem;

public interface MenuService {

    List<Menu> getAllMenus(String restaurantId);

    Menu getMenuById(String restaurantId, String menuId);

    Menu createMenu(String restaurantId, MenuCommand command);

    Menu updateMenu(String restaurantId, String menuId, MenuCommand command);

    void deleteMenu(String restaurantId, String menuId);

    MenuItem addMenuItem(String restaurantId, String menuId, MenuItemCommand command);

    MenuItem updateMenuItem(String restaurantId, String menuId, String itemId, MenuItemCommand command);

    void removeMenuItem(String restaurantId, String menuId, String itemId);

    record MenuCommand(String name, String description, int position) {
    }

    record MenuItemCommand(String name, String description, long priceCents, String imageUrl,
                           boolean available) {
    }
}

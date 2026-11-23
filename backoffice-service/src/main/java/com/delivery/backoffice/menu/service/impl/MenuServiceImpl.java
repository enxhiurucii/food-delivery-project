package com.delivery.backoffice.menu.service.impl;

import java.time.Clock;
import java.util.List;

import com.delivery.backoffice.menu.model.Menu;
import com.delivery.backoffice.menu.model.MenuItem;
import com.delivery.backoffice.menu.repository.MenuRepository;
import com.delivery.backoffice.menu.service.MenuService;
import com.delivery.backoffice.restaurant.service.RestaurantService;
import com.delivery.backoffice.shared.error.NotFoundException;

import org.springframework.stereotype.Service;

@Service
public class MenuServiceImpl implements MenuService {

    private final MenuRepository repository;
    private final RestaurantService restaurants;
    private final Clock clock;

    public MenuServiceImpl(MenuRepository repository, RestaurantService restaurants, Clock clock) {
        this.repository = repository;
        this.restaurants = restaurants;
        this.clock = clock;
    }

    @Override
    public List<Menu> getAllMenus(String restaurantId) {
        return repository.findByRestaurantIdOrderByPositionAsc(restaurantId);
    }

    @Override
    public Menu getMenuById(String restaurantId, String menuId) {
        // Always scoped by restaurant: a menu id from another restaurant behaves like a missing one.
        return repository.findByIdAndRestaurantId(menuId, restaurantId)
                .orElseThrow(() -> new NotFoundException("Menu", menuId));
    }

    @Override
    public Menu createMenu(String restaurantId, MenuCommand command) {
        restaurants.getRestaurantById(restaurantId);
        return repository.save(Menu.create(restaurantId, command.name(), command.description(), command.position(),
                clock.instant()));
    }

    @Override
    public Menu updateMenu(String restaurantId, String menuId, MenuCommand command) {
        Menu menu = getMenuById(restaurantId, menuId);
        menu.rename(command.name(), command.description(), command.position(), clock.instant());
        return repository.save(menu);
    }

    @Override
    public void deleteMenu(String restaurantId, String menuId) {
        repository.delete(getMenuById(restaurantId, menuId));
    }

    @Override
    public MenuItem addMenuItem(String restaurantId, String menuId, MenuItemCommand command) {
        Menu menu = getMenuById(restaurantId, menuId);
        MenuItem item = menu.addItem(command.name(), command.description(), command.priceCents(), command.imageUrl(),
                command.available(), clock.instant());
        repository.save(menu);
        return item;
    }

    @Override
    public MenuItem updateMenuItem(String restaurantId, String menuId, String itemId, MenuItemCommand command) {
        Menu menu = getMenuById(restaurantId, menuId);
        MenuItem item = menu.updateItem(itemId, command.name(), command.description(), command.priceCents(),
                command.imageUrl(), command.available(), clock.instant());
        repository.save(menu);
        return item;
    }

    @Override
    public void removeMenuItem(String restaurantId, String menuId, String itemId) {
        Menu menu = getMenuById(restaurantId, menuId);
        menu.removeItem(itemId, clock.instant());
        repository.save(menu);
    }
}

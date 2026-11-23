package com.delivery.webshop.catalog.controller;

import java.util.List;

import com.delivery.webshop.catalog.dto.MenuView;
import com.delivery.webshop.catalog.dto.RestaurantView;
import com.delivery.webshop.catalog.service.CatalogService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public restaurant browsing (no login needed). Data comes from the back office over HTTP. */
@RestController
@RequestMapping("/api/restaurants")
public class CatalogController {

    private final CatalogService backoffice;

    public CatalogController(CatalogService backoffice) {
        this.backoffice = backoffice;
    }

    @GetMapping
    public List<RestaurantView> getAllRestaurants() {
        return backoffice.getAllRestaurants().stream().map(RestaurantView::from).toList();
    }

    @GetMapping("/{restaurantId}")
    public RestaurantView getRestaurantById(@PathVariable String restaurantId) {
        return RestaurantView.from(backoffice.getRestaurantById(restaurantId));
    }

    @GetMapping("/{restaurantId}/menus")
    public List<MenuView> getMenusByRestaurant(@PathVariable String restaurantId) {
        return backoffice.getMenusByRestaurant(restaurantId).stream().map(MenuView::from).toList();
    }
}

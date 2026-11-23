package com.delivery.backoffice.catalog.service;

import java.util.List;

import com.delivery.backoffice.catalog.dto.CatalogMenu;
import com.delivery.backoffice.catalog.dto.CatalogRestaurant;
import com.delivery.backoffice.catalog.dto.PricingRequest;
import com.delivery.backoffice.catalog.dto.PricingResponse;

/** Read-only projection of restaurants and menus for the customer-facing webshop. */
public interface CatalogService {

    List<CatalogRestaurant> getAllRestaurants();

    CatalogRestaurant getRestaurantById(String restaurantId);

    List<CatalogMenu> getMenusByRestaurant(String restaurantId);

    PricingResponse calculatePrice(String restaurantId, PricingRequest request);
}

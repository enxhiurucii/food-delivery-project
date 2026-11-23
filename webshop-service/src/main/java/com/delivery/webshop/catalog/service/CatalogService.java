package com.delivery.webshop.catalog.service;

import java.util.List;

import com.delivery.webshop.catalog.dto.CatalogMenu;
import com.delivery.webshop.catalog.dto.CatalogRestaurant;
import com.delivery.webshop.catalog.dto.PricingRequest;
import com.delivery.webshop.catalog.dto.PricingResponse;

public interface CatalogService {

    List<CatalogRestaurant> getAllRestaurants();

    CatalogRestaurant getRestaurantById(String restaurantId);

    List<CatalogMenu> getMenusByRestaurant(String restaurantId);

    PricingResponse calculatePrice(String restaurantId, PricingRequest request);
}

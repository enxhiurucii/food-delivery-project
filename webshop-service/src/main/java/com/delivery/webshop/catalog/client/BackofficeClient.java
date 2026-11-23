package com.delivery.webshop.catalog.client;

import java.util.List;

import com.delivery.webshop.catalog.dto.CatalogMenu;
import com.delivery.webshop.catalog.dto.CatalogRestaurant;
import com.delivery.webshop.catalog.dto.PricingRequest;
import com.delivery.webshop.catalog.dto.PricingResponse;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/** Declarative HTTP client (Spring HTTP interface) for the back office's internal catalog API. */
@HttpExchange("/internal/restaurants")
public interface BackofficeClient {

    @GetExchange
    List<CatalogRestaurant> getAllRestaurants();

    @GetExchange("/{restaurantId}")
    CatalogRestaurant getRestaurantById(@PathVariable String restaurantId);

    @GetExchange("/{restaurantId}/menus")
    List<CatalogMenu> getMenusByRestaurant(@PathVariable String restaurantId);

    @PostExchange("/{restaurantId}/pricing")
    PricingResponse calculatePrice(@PathVariable String restaurantId, @RequestBody PricingRequest request);
}

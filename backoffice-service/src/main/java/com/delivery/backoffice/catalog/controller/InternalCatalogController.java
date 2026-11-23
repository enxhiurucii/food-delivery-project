package com.delivery.backoffice.catalog.controller;

import java.util.List;

import com.delivery.backoffice.catalog.dto.CatalogMenu;
import com.delivery.backoffice.catalog.dto.CatalogRestaurant;
import com.delivery.backoffice.catalog.dto.PricingRequest;
import com.delivery.backoffice.catalog.dto.PricingResponse;
import com.delivery.backoffice.catalog.service.CatalogService;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service API used only by the webshop (role CATALOG_READER, see SecurityConfig).
 * Not routed by nginx. Amounts are in cents: machine-to-machine contract, no rounding ambiguity.
 */
@RestController
@RequestMapping("/internal/restaurants")
public class InternalCatalogController {

    private final CatalogService catalog;

    public InternalCatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public List<CatalogRestaurant> getAllRestaurants() {
        return catalog.getAllRestaurants();
    }

    @GetMapping("/{restaurantId}")
    public CatalogRestaurant getRestaurantById(@PathVariable String restaurantId) {
        return catalog.getRestaurantById(restaurantId);
    }

    @GetMapping("/{restaurantId}/menus")
    public List<CatalogMenu> getMenusByRestaurant(@PathVariable String restaurantId) {
        return catalog.getMenusByRestaurant(restaurantId);
    }

    /** Authoritative prices and availability for checkout. Never trust prices coming from a browser. */
    @PostMapping("/{restaurantId}/pricing")
    public PricingResponse calculatePrice(@PathVariable String restaurantId, @Valid @RequestBody PricingRequest request) {
        return catalog.calculatePrice(restaurantId, request);
    }
}

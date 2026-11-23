package com.delivery.backoffice.restaurant.controller;

import java.util.List;

import com.delivery.backoffice.restaurant.dto.RestaurantRequest;
import com.delivery.backoffice.restaurant.dto.RestaurantResponse;
import com.delivery.backoffice.restaurant.service.RestaurantService;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/restaurants")
public class RestaurantController {

    private final RestaurantService service;

    public RestaurantController(RestaurantService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<RestaurantResponse> getAllRestaurants() {
        return service.getAllRestaurants().stream().map(RestaurantResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public RestaurantResponse createRestaurant(@Valid @RequestBody RestaurantRequest request) {
        return RestaurantResponse.from(service.createRestaurant(request.toDetails()));
    }

    @GetMapping("/{restaurantId}")
    @PreAuthorize("@tenantGuard.canAccess(#restaurantId, authentication)")
    public RestaurantResponse getRestaurantById(@PathVariable String restaurantId) {
        return RestaurantResponse.from(service.getRestaurantById(restaurantId));
    }

    @PutMapping("/{restaurantId}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN') and @tenantGuard.canAccess(#restaurantId, authentication)")
    public RestaurantResponse updateRestaurant(@PathVariable String restaurantId, @Valid @RequestBody RestaurantRequest request) {
        return RestaurantResponse.from(service.updateRestaurant(restaurantId, request.toDetails()));
    }
}

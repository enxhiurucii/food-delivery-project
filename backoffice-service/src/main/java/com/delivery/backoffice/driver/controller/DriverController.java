package com.delivery.backoffice.driver.controller;

import java.util.List;

import com.delivery.backoffice.driver.dto.DriverOrderResponse;
import com.delivery.backoffice.driver.service.DriverService;
import com.delivery.backoffice.order.dto.ChangeStatusRequest;
import com.delivery.backoffice.order.model.Order;
import com.delivery.backoffice.restaurant.model.Restaurant;
import com.delivery.backoffice.restaurant.repository.RestaurantRepository;
import com.delivery.backoffice.shared.error.NotFoundException;
import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/driver")
public class DriverController {

    private final DriverService driverService;
    private final RestaurantRepository restaurants;

    public DriverController(DriverService driverService, RestaurantRepository restaurants) {
        this.driverService = driverService;
        this.restaurants = restaurants;
    }

    @GetMapping("/deliveries/available")
    public List<DriverOrderResponse> getAvailableDeliveries() {
        return driverService.getAvailableDeliveries().stream()
                .map(o -> DriverOrderResponse.from(o, findRestaurant(o.getRestaurantId())))
                .toList();
    }

    @GetMapping("/deliveries/active")
    public DriverOrderResponse getActiveDelivery(Authentication auth) {
        String driverId = driverSubject(auth);
        return driverService.getActiveDelivery(driverId)
                .map(o -> DriverOrderResponse.from(o, findRestaurant(o.getRestaurantId())))
                .orElse(null);
    }

    @PostMapping("/deliveries/{orderId}/claim")
    public DriverOrderResponse claimDelivery(@PathVariable String orderId, Authentication auth) {
        Jwt jwt = (Jwt) auth.getPrincipal();
        String driverId = jwt.getSubject();
        String driverName = jwt.getClaimAsString("name") != null
                ? jwt.getClaimAsString("name")
                : jwt.getClaimAsString("preferred_username");
        Order order = driverService.claimDelivery(orderId, driverId, driverName);
        return DriverOrderResponse.from(order, findRestaurant(order.getRestaurantId()));
    }

    @PostMapping("/deliveries/{orderId}/status")
    public DriverOrderResponse updateStatus(@PathVariable String orderId,
                                             @Valid @RequestBody ChangeStatusRequest request,
                                             Authentication auth) {
        String driverId = driverSubject(auth);
        Order order = driverService.updateDeliveryStatus(orderId, driverId, request.status());
        return DriverOrderResponse.from(order, findRestaurant(order.getRestaurantId()));
    }

    private Restaurant findRestaurant(String restaurantId) {
        return restaurants.findById(restaurantId)
                .orElseThrow(() -> new NotFoundException("Restaurant", restaurantId));
    }

    private static String driverSubject(Authentication auth) {
        return ((Jwt) auth.getPrincipal()).getSubject();
    }
}

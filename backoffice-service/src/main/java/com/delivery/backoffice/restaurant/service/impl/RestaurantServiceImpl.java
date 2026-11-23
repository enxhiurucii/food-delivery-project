package com.delivery.backoffice.restaurant.service.impl;

import java.time.Clock;
import java.util.List;

import com.delivery.backoffice.restaurant.model.Restaurant;
import com.delivery.backoffice.restaurant.model.RestaurantDetails;
import com.delivery.backoffice.restaurant.repository.RestaurantRepository;
import com.delivery.backoffice.restaurant.service.RestaurantService;
import com.delivery.backoffice.shared.error.NotFoundException;

import org.springframework.stereotype.Service;

@Service
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository repository;
    private final Clock clock;

    public RestaurantServiceImpl(RestaurantRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public List<Restaurant> getAllRestaurants() {
        return repository.findAll();
    }

    @Override
    public Restaurant getRestaurantById(String id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Restaurant", id));
    }

    @Override
    public Restaurant createRestaurant(RestaurantDetails details) {
        return repository.save(Restaurant.create(null, details, clock.instant()));
    }

    @Override
    public Restaurant updateRestaurant(String id, RestaurantDetails details) {
        Restaurant restaurant = getRestaurantById(id);
        restaurant.update(details, clock.instant());
        return repository.save(restaurant);
    }
}

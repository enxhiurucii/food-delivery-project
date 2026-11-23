package com.delivery.backoffice.restaurant.service;

import java.util.List;

import com.delivery.backoffice.restaurant.model.Restaurant;
import com.delivery.backoffice.restaurant.model.RestaurantDetails;

public interface RestaurantService {

    List<Restaurant> getAllRestaurants();

    Restaurant getRestaurantById(String id);

    Restaurant createRestaurant(RestaurantDetails details);

    Restaurant updateRestaurant(String id, RestaurantDetails details);
}

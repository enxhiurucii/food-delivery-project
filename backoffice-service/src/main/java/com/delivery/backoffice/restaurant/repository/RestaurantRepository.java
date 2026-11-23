package com.delivery.backoffice.restaurant.repository;

import com.delivery.backoffice.restaurant.model.Restaurant;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface RestaurantRepository extends MongoRepository<Restaurant, String> {
}

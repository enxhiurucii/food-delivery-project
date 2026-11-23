package com.delivery.backoffice.menu.repository;

import java.util.List;
import java.util.Optional;

import com.delivery.backoffice.menu.model.Menu;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MenuRepository extends MongoRepository<Menu, String> {

    List<Menu> findByRestaurantIdOrderByPositionAsc(String restaurantId);

    Optional<Menu> findByIdAndRestaurantId(String id, String restaurantId);
}

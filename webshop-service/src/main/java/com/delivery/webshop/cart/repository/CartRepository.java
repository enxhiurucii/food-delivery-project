package com.delivery.webshop.cart.repository;

import com.delivery.webshop.cart.model.Cart;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface CartRepository extends MongoRepository<Cart, String> {
}

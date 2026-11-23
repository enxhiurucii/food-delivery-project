package com.delivery.webshop.cart.service;

import com.delivery.webshop.cart.model.Cart;

public interface CartService {

    Cart getCartByCustomer(String customerId);

    Cart addItemToCart(String customerId, String restaurantId, String menuItemId, int quantity);

    Cart changeItemQuantity(String customerId, String menuItemId, int quantity);

    void clearCart(String customerId);
}

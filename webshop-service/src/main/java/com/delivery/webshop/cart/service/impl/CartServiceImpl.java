package com.delivery.webshop.cart.service.impl;

import java.time.Clock;
import java.util.List;

import com.delivery.webshop.cart.model.Cart;
import com.delivery.webshop.cart.repository.CartRepository;
import com.delivery.webshop.cart.service.CartService;
import com.delivery.webshop.catalog.dto.PricedLine;
import com.delivery.webshop.catalog.dto.PricingRequest;
import com.delivery.webshop.catalog.dto.RequestedLine;
import com.delivery.webshop.catalog.service.CatalogService;

import org.springframework.stereotype.Service;

@Service
public class CartServiceImpl implements CartService {

    private final CartRepository carts;
    private final CatalogService backoffice;
    private final Clock clock;

    public CartServiceImpl(CartRepository carts, CatalogService backoffice, Clock clock) {
        this.carts = carts;
        this.backoffice = backoffice;
        this.clock = clock;
    }

    @Override
    public Cart getCartByCustomer(String customerId) {
        return carts.findById(customerId).orElseGet(() -> Cart.emptyFor(customerId, clock.instant()));
    }

    /** Validates the item against the back office (exists, available) and snapshots name + price for display. */
    @Override
    public Cart addItemToCart(String customerId, String restaurantId, String menuItemId, int quantity) {
        PricedLine priced = backoffice.calculatePrice(restaurantId,
                new PricingRequest(List.of(new RequestedLine(menuItemId, quantity)))).lines().getFirst();
        Cart cart = getCartByCustomer(customerId);
        cart.addItem(restaurantId, menuItemId, priced.name(), priced.unitPriceCents(), quantity, clock.instant());
        return carts.save(cart);
    }

    @Override
    public Cart changeItemQuantity(String customerId, String menuItemId, int quantity) {
        Cart cart = getCartByCustomer(customerId);
        cart.changeQuantity(menuItemId, quantity, clock.instant());
        return carts.save(cart);
    }

    @Override
    public void clearCart(String customerId) {
        carts.deleteById(customerId);
    }
}

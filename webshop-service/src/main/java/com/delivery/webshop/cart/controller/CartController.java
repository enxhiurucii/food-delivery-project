package com.delivery.webshop.cart.controller;

import com.delivery.webshop.cart.dto.AddItemRequest;
import com.delivery.webshop.cart.dto.CartView;
import com.delivery.webshop.cart.dto.ChangeQuantityRequest;
import com.delivery.webshop.cart.service.CartService;
import com.delivery.webshop.shared.security.CurrentCustomer;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The logged-in customer's cart. The owner always comes from the token, never from the request. */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService carts;

    public CartController(CartService carts) {
        this.carts = carts;
    }

    @GetMapping
    public CartView getCart(@AuthenticationPrincipal Jwt jwt) {
        return CartView.from(carts.getCartByCustomer(CurrentCustomer.from(jwt).id()));
    }

    @PostMapping("/items")
    public CartView addItemToCart(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddItemRequest request) {
        return CartView.from(carts.addItemToCart(CurrentCustomer.from(jwt).id(), request.restaurantId(),
                request.menuItemId(), request.quantity()));
    }

    @PutMapping("/items/{menuItemId}")
    public CartView changeItemQuantity(@AuthenticationPrincipal Jwt jwt, @PathVariable String menuItemId,
                            @Valid @RequestBody ChangeQuantityRequest request) {
        return CartView.from(carts.changeItemQuantity(CurrentCustomer.from(jwt).id(), menuItemId, request.quantity()));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearCart(@AuthenticationPrincipal Jwt jwt) {
        carts.clearCart(CurrentCustomer.from(jwt).id());
    }
}

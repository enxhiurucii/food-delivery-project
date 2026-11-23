package com.delivery.webshop.cart.dto;

import java.math.BigDecimal;
import java.util.List;

import com.delivery.webshop.cart.model.Cart;
import com.delivery.webshop.shared.money.Money;

public record CartView(String restaurantId, List<CartItemView> items, BigDecimal subtotal, String currency) {

    public static CartView from(Cart cart) {
        return new CartView(cart.getRestaurantId(),
                cart.getItems().stream()
                        .map(i -> new CartItemView(i.menuItemId(), i.name(), Money.toDecimal(i.unitPriceCents()),
                                i.quantity(), Money.toDecimal(i.totalCents())))
                        .toList(),
                Money.toDecimal(cart.subtotalCents()), Money.CURRENCY);
    }
}

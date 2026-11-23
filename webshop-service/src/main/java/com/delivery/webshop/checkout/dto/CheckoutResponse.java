package com.delivery.webshop.checkout.dto;

import com.delivery.webshop.order.dto.OrderView;

/** {@code pricesChanged}: the menu changed since items were added; the order uses the current prices. */
public record CheckoutResponse(OrderView order, boolean pricesChanged) {
}

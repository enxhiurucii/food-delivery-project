package com.delivery.webshop.cart.dto;

import java.math.BigDecimal;

public record CartItemView(String menuItemId, String name, BigDecimal unitPrice, int quantity, BigDecimal total) {
}

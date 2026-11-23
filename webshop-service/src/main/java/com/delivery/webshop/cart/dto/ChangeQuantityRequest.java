package com.delivery.webshop.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import static com.delivery.webshop.cart.model.Cart.MAX_QUANTITY_PER_ITEM;

public record ChangeQuantityRequest(@Min(0) @Max(MAX_QUANTITY_PER_ITEM) int quantity) {
}

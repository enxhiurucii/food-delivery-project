package com.delivery.webshop.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import static com.delivery.webshop.cart.model.Cart.MAX_QUANTITY_PER_ITEM;

public record AddItemRequest(@NotBlank String restaurantId, @NotBlank String menuItemId,
                             @Min(1) @Max(MAX_QUANTITY_PER_ITEM) int quantity) {
}

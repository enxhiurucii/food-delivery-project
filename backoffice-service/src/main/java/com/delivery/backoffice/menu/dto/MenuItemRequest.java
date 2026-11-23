package com.delivery.backoffice.menu.dto;

import java.math.BigDecimal;

import com.delivery.backoffice.menu.service.MenuService.MenuItemCommand;
import com.delivery.backoffice.shared.money.Money;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MenuItemRequest(
        @NotBlank String name,
        String description,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        String imageUrl,
        boolean available) {

    public MenuItemCommand toCommand() {
        return new MenuItemCommand(name, description, Money.toCents(price), imageUrl, available);
    }
}

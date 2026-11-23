package com.delivery.backoffice.menu.dto;

import com.delivery.backoffice.menu.service.MenuService.MenuCommand;
import jakarta.validation.constraints.NotBlank;

public record MenuRequest(@NotBlank String name, String description, int position) {
    public MenuCommand toCommand() {
        return new MenuCommand(name, description, position);
    }
}

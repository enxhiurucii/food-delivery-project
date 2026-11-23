package com.delivery.backoffice.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RequestedLine(@NotBlank String menuItemId, @Min(1) int quantity) {
}

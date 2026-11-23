package com.delivery.backoffice.order.dto;

import com.delivery.backoffice.order.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(@NotNull OrderStatus status) {
}

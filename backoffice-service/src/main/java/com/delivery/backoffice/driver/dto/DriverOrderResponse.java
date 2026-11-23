package com.delivery.backoffice.driver.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.delivery.backoffice.order.enums.OrderStatus;
import com.delivery.backoffice.order.model.DeliveryAddress;
import com.delivery.backoffice.order.model.Order;
import com.delivery.backoffice.restaurant.model.Restaurant;
import com.delivery.backoffice.shared.money.Money;

public record DriverOrderResponse(
        String id,
        String restaurantId,
        String restaurantName,
        String restaurantAddress,
        String customerName,
        List<Line> lines,
        BigDecimal total,
        String currency,
        DeliveryAddress deliveryAddress,
        String notes,
        String paymentMethod,
        String paymentTiming,
        String paymentStatus,
        OrderStatus status,
        List<OrderStatus> allowedNextStatuses,
        String driverId,
        String driverName,
        Instant placedAt) {

    public record Line(String name, int quantity, BigDecimal total) {}

    public static DriverOrderResponse from(Order order, Restaurant restaurant) {
        List<OrderStatus> transitions = switch (order.getStatus()) {
            case READY_FOR_PICKUP -> List.of(OrderStatus.OUT_FOR_DELIVERY);
            case OUT_FOR_DELIVERY -> List.of(OrderStatus.DELIVERED);
            default -> List.of();
        };
        return new DriverOrderResponse(
                order.getId(),
                order.getRestaurantId(),
                restaurant.getName(),
                restaurant.getAddress(),
                order.getCustomerName(),
                order.getLines().stream()
                        .map(l -> new Line(l.name(), l.quantity(), Money.toDecimal(l.totalCents())))
                        .toList(),
                Money.toDecimal(order.getTotalCents()),
                Money.CURRENCY,
                order.getDeliveryAddress(),
                order.getNotes(),
                order.getPayment().method().name(),
                order.getPayment().timing().name(),
                order.getPayment().status().name(),
                order.getStatus(),
                transitions,
                order.getDriverId(),
                order.getDriverName(),
                order.getPlacedAt());
    }
}

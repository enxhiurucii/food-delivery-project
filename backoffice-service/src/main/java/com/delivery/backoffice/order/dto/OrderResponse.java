package com.delivery.backoffice.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.delivery.backoffice.order.enums.OrderStatus;
import com.delivery.backoffice.order.model.DeliveryAddress;
import com.delivery.backoffice.order.model.Order;
import com.delivery.backoffice.order.model.StatusChange;
import com.delivery.backoffice.shared.money.Money;

public record OrderResponse(
        String id,
        String restaurantId,
        String customerName,
        List<Line> lines,
        BigDecimal subtotal,
        BigDecimal deliveryFee,
        BigDecimal total,
        String currency,
        String fulfillmentType,
        DeliveryAddress deliveryAddress,
        String notes,
        String paymentMethod,
        String paymentTiming,
        String paymentStatus,
        OrderStatus status,
        List<OrderStatus> allowedNextStatuses,
        List<StatusChange> statusHistory,
        Instant placedAt,
        Instant closedAt,
        String driverId) {

    public record Line(String menuItemId, String name, BigDecimal unitPrice, int quantity, BigDecimal total) {
    }

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getRestaurantId(),
                order.getCustomerName(),
                order.getLines().stream()
                        .map(l -> new Line(l.menuItemId(), l.name(), Money.toDecimal(l.unitPriceCents()), l.quantity(),
                                Money.toDecimal(l.totalCents())))
                        .toList(),
                Money.toDecimal(order.getSubtotalCents()),
                Money.toDecimal(order.getDeliveryFeeCents()),
                Money.toDecimal(order.getTotalCents()),
                Money.CURRENCY,
                order.getFulfillmentType().name(),
                order.getDeliveryAddress(),
                order.getNotes(),
                order.getPayment().method().name(),
                order.getPayment().timing().name(),
                order.getPayment().status().name(),
                order.getStatus(),
                // Lets the UI render exactly the buttons that are valid right now (staff transitions only).
                order.getStatus().staffTransitions(order.getFulfillmentType()),
                order.getStatusHistory(),
                order.getPlacedAt(),
                order.getClosedAt(),
                order.getDriverId());
    }
}

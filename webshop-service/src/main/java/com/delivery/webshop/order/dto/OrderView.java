package com.delivery.webshop.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.delivery.webshop.order.enums.FulfillmentType;
import com.delivery.webshop.order.enums.OrderStatus;
import com.delivery.webshop.order.model.CustomerOrder;
import com.delivery.webshop.order.model.DeliveryAddress;
import com.delivery.webshop.order.model.StatusChange;
import com.delivery.webshop.shared.money.Money;

/**
 * What the "Your order was received" page renders. {@code timeline} is ready to draw: the expected steps
 * for this fulfillment type, each with a reached flag and timestamp, so the frontend needs no lifecycle logic.
 */
public record OrderView(
        String id,
        String restaurantId,
        String restaurantName,
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
        boolean finished,
        List<TimelineStep> timeline,
        Instant placedAt,
        Instant estimatedReadyAt) {

    public record Line(String name, BigDecimal unitPrice, int quantity, BigDecimal total) {
    }

    public record TimelineStep(OrderStatus status, boolean reached, boolean current, Instant at) {
    }

    public static OrderView from(CustomerOrder order) {
        return new OrderView(
                order.getId(),
                order.getRestaurantId(),
                order.getRestaurantName(),
                order.getLines().stream()
                        .map(l -> new Line(l.name(), Money.toDecimal(l.unitPriceCents()), l.quantity(),
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
                order.getStatus().isTerminal(),
                timeline(order),
                order.getPlacedAt(),
                order.getEstimatedReadyAt());
    }

    static List<TimelineStep> timeline(CustomerOrder order) {
        Map<OrderStatus, Instant> reachedAt = order.getStatusHistory().stream()
                .collect(Collectors.toMap(StatusChange::status, StatusChange::at, (first, second) -> first));
        List<TimelineStep> steps = new ArrayList<>();
        boolean derailed = order.getStatus() == OrderStatus.REJECTED || order.getStatus() == OrderStatus.CANCELLED;
        for (OrderStatus step : OrderStatus.happyPath(order.getFulfillmentType())) {
            if (derailed && !reachedAt.containsKey(step)) {
                continue; // a rejected/cancelled order shows only what really happened
            }
            steps.add(new TimelineStep(step, reachedAt.containsKey(step), step == order.getStatus(), reachedAt.get(step)));
        }
        if (derailed) {
            steps.add(new TimelineStep(order.getStatus(), true, true, reachedAt.get(order.getStatus())));
        }
        return steps;
    }
}

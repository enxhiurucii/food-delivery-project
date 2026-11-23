package com.delivery.backoffice.order.listener;

import com.delivery.backoffice.order.enums.FulfillmentType;
import com.delivery.backoffice.order.model.DeliveryAddress;
import com.delivery.backoffice.order.model.Order;
import com.delivery.backoffice.order.model.OrderChanged;
import com.delivery.backoffice.order.model.OrderLine;
import com.delivery.backoffice.order.model.Payment;
import com.delivery.backoffice.order.repository.OrderRepository;
import com.delivery.backoffice.shared.messaging.event.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Turns the webshop's "order.placed" event into a kitchen order. Idempotent: redeliveries are ignored. */
@Service
public class OrderPlacedHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderPlacedHandler.class);

    private final OrderRepository repository;
    private final ApplicationEventPublisher events;

    public OrderPlacedHandler(OrderRepository repository, ApplicationEventPublisher events) {
        this.repository = repository;
        this.events = events;
    }

    @Transactional
    public void handle(OrderPlacedEvent event) {
        if (repository.existsById(event.orderId())) {
            log.info("Order {} already received (event {}), ignoring duplicate", event.orderId(), event.eventId());
            return;
        }
        FulfillmentType fulfillment = FulfillmentType.valueOf(event.fulfillmentType());
        Order order = Order.place(
                event.orderId(),
                event.restaurantId(),
                event.customerId(),
                event.customerName(),
                event.lines().stream()
                        .map(l -> new OrderLine(l.menuItemId(), l.name(), l.unitPriceCents(), l.quantity()))
                        .toList(),
                event.deliveryFeeCents(),
                fulfillment,
                toAddress(event.deliveryAddress()),
                event.notes(),
                new Payment(Payment.Method.valueOf(event.paymentMethod()),
                        Payment.Timing.valueOf(event.paymentTiming()),
                        Payment.Status.valueOf(event.paymentStatus())),
                event.occurredAt());
        if (order.getTotalCents() != event.totalCents()) {
            // Both sides compute totals from the same snapshot; a mismatch means a contract bug, not a user error.
            log.warn("Order {} total mismatch: webshop={} backoffice={}", order.getId(), event.totalCents(),
                    order.getTotalCents());
        }
        Order saved = repository.save(order);
        events.publishEvent(new OrderChanged(saved, OrderChanged.Type.PLACED));
        log.info("Order {} received for restaurant {}", saved.getId(), saved.getRestaurantId());
    }

    private static DeliveryAddress toAddress(OrderPlacedEvent.Address address) {
        return address == null ? null : new DeliveryAddress(address.street(), address.city(), address.postalCode(),
                address.phone(), address.instructions());
    }
}

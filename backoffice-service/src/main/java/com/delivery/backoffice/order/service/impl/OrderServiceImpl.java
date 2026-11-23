package com.delivery.backoffice.order.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.delivery.backoffice.order.enums.OrderStatus;
import com.delivery.backoffice.order.model.Order;
import com.delivery.backoffice.order.model.OrderChanged;
import com.delivery.backoffice.order.repository.OrderRepository;
import com.delivery.backoffice.order.service.OrderService;
import com.delivery.backoffice.shared.error.NotFoundException;
import com.delivery.backoffice.shared.messaging.RoutingKeys;
import com.delivery.backoffice.shared.messaging.event.OrderStatusChangedEvent;
import com.delivery.backoffice.shared.messaging.outbox.Outbox;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository repository;
    private final Outbox outbox;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public OrderServiceImpl(OrderRepository repository, Outbox outbox, ApplicationEventPublisher events, Clock clock) {
        this.repository = repository;
        this.outbox = outbox;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public List<Order> getOrdersByRestaurant(String restaurantId, Collection<OrderStatus> statuses) {
        Collection<OrderStatus> filter = statuses == null || statuses.isEmpty() ? ACTIVE_STATUSES : statuses;
        return repository.findByRestaurantIdAndStatusInOrderByPlacedAtDesc(restaurantId, filter);
    }

    @Override
    public Order getOrderById(String restaurantId, String orderId) {
        return repository.findByIdAndRestaurantId(orderId, restaurantId)
                .orElseThrow(() -> new NotFoundException("Order", orderId));
    }

    /**
     * Moves the order forward. The state change and the integration event are committed atomically
     * (transactional outbox); the optimistic lock (@Version) rejects concurrent updates with 409.
     */
    @Override
    @Transactional
    public Order updateOrderStatus(String restaurantId, String orderId, OrderStatus next) {
        Order order = getOrderById(restaurantId, orderId);
        Instant now = clock.instant();
        order.changeStatus(next, now);
        Order saved = repository.save(order);

        UUID eventId = UUID.randomUUID();
        outbox.enqueue(RoutingKeys.ORDER_STATUS_CHANGED, eventId, new OrderStatusChangedEvent(
                eventId, now, saved.getId(), saved.getRestaurantId(), next.name(), saved.statusSequence(),
                saved.getPayment().status().name(), now));
        events.publishEvent(new OrderChanged(saved, OrderChanged.Type.STATUS_CHANGED));
        return saved;
    }
}

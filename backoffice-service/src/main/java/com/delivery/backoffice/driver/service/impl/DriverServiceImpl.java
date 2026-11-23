package com.delivery.backoffice.driver.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.delivery.backoffice.driver.service.DriverService;
import com.delivery.backoffice.order.enums.FulfillmentType;
import com.delivery.backoffice.order.enums.OrderStatus;
import com.delivery.backoffice.order.model.Order;
import com.delivery.backoffice.order.model.OrderChanged;
import com.delivery.backoffice.order.repository.OrderRepository;
import com.delivery.backoffice.shared.error.BusinessRuleException;
import com.delivery.backoffice.shared.error.NotFoundException;
import com.delivery.backoffice.shared.messaging.RoutingKeys;
import com.delivery.backoffice.shared.messaging.event.OrderStatusChangedEvent;
import com.delivery.backoffice.shared.messaging.outbox.Outbox;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DriverServiceImpl implements DriverService {

    private static final Set<OrderStatus> ACTIVE_DRIVER_STATUSES = EnumSet.of(
            OrderStatus.READY_FOR_PICKUP, OrderStatus.OUT_FOR_DELIVERY);

    private final OrderRepository repository;
    private final Outbox outbox;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public DriverServiceImpl(OrderRepository repository, Outbox outbox, ApplicationEventPublisher events, Clock clock) {
        this.repository = repository;
        this.outbox = outbox;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public List<Order> getAvailableDeliveries() {
        return repository.findByFulfillmentTypeAndStatusAndDriverIdIsNullOrderByPlacedAtAsc(
                FulfillmentType.DELIVERY, OrderStatus.READY_FOR_PICKUP);
    }

    @Override
    public Optional<Order> getActiveDelivery(String driverId) {
        return repository.findByDriverIdAndStatusIn(driverId, ACTIVE_DRIVER_STATUSES);
    }

    @Override
    public Order claimDelivery(String orderId, String driverId, String driverName) {
        Order order = repository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order", orderId));
        if (order.getFulfillmentType() != FulfillmentType.DELIVERY) {
            throw new BusinessRuleException("NOT_DELIVERY", "This order is not a delivery order");
        }
        if (order.getStatus() != OrderStatus.READY_FOR_PICKUP) {
            throw new BusinessRuleException("NOT_AVAILABLE", "Order is not available for pickup");
        }
        if (order.hasDriver()) {
            throw new BusinessRuleException("ALREADY_CLAIMED", "This order has already been claimed by another driver");
        }
        order.assignDriver(driverId, driverName);
        try {
            return repository.save(order);
        } catch (OptimisticLockingFailureException e) {
            // Two drivers raced; re-read to give the accurate business error
            Order current = repository.findById(orderId).orElseThrow(() -> new NotFoundException("Order", orderId));
            if (current.hasDriver()) {
                throw new BusinessRuleException("ALREADY_CLAIMED", "This order was just claimed by another driver");
            }
            throw e; // something else changed — let the global handler return 409
        }
    }

    @Override
    @Transactional
    public Order updateDeliveryStatus(String orderId, String driverId, OrderStatus next) {
        Order order = repository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order", orderId));
        if (!driverId.equals(order.getDriverId())) {
            throw new BusinessRuleException("NOT_YOUR_DELIVERY", "You are not assigned to this delivery");
        }
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

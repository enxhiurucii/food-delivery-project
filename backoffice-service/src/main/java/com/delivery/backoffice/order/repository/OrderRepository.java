package com.delivery.backoffice.order.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.delivery.backoffice.order.enums.FulfillmentType;
import com.delivery.backoffice.order.enums.OrderStatus;
import com.delivery.backoffice.order.model.Order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrderRepository extends MongoRepository<Order, String> {

    Optional<Order> findByIdAndRestaurantId(String id, String restaurantId);

    List<Order> findByRestaurantIdAndStatusInOrderByPlacedAtDesc(String restaurantId, Collection<OrderStatus> statuses);

    Page<Order> findByRestaurantIdAndStatusInAndClosedAtBetween(String restaurantId, Collection<OrderStatus> statuses,
                                                                Instant from, Instant to, Pageable pageable);

    // Available for drivers: DELIVERY orders READY_FOR_PICKUP with no driver assigned yet
    List<Order> findByFulfillmentTypeAndStatusAndDriverIdIsNullOrderByPlacedAtAsc(FulfillmentType fulfillmentType, OrderStatus status);

    // Driver's currently active delivery
    Optional<Order> findByDriverIdAndStatusIn(String driverId, Collection<OrderStatus> statuses);
}

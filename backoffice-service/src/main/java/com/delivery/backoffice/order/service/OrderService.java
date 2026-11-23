package com.delivery.backoffice.order.service;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.delivery.backoffice.order.enums.OrderStatus;
import com.delivery.backoffice.order.model.Order;

public interface OrderService {

    /** What the kitchen board shows by default. */
    Set<OrderStatus> ACTIVE_STATUSES = EnumSet.of(
            OrderStatus.PLACED, OrderStatus.ACCEPTED, OrderStatus.PREPARING,
            OrderStatus.READY_FOR_PICKUP, OrderStatus.OUT_FOR_DELIVERY);

    List<Order> getOrdersByRestaurant(String restaurantId, Collection<OrderStatus> statuses);

    Order getOrderById(String restaurantId, String orderId);

    Order updateOrderStatus(String restaurantId, String orderId, OrderStatus next);
}

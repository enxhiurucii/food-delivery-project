package com.delivery.backoffice.driver.service;

import java.util.List;
import java.util.Optional;

import com.delivery.backoffice.order.enums.OrderStatus;
import com.delivery.backoffice.order.model.Order;

public interface DriverService {
    List<Order> getAvailableDeliveries();
    Optional<Order> getActiveDelivery(String driverId);
    Order claimDelivery(String orderId, String driverId, String driverName);
    Order updateDeliveryStatus(String orderId, String driverId, OrderStatus next);
}

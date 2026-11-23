package com.delivery.webshop.order.service;

import java.util.List;

import com.delivery.webshop.order.model.CustomerOrder;

public interface OrderTrackingService {

    List<CustomerOrder> getOrdersByCustomer(String customerId);

    CustomerOrder getOrderByCustomer(String customerId, String orderId);
}

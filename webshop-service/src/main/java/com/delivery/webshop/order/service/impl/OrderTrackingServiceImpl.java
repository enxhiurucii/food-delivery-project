package com.delivery.webshop.order.service.impl;

import java.util.List;

import com.delivery.webshop.order.model.CustomerOrder;
import com.delivery.webshop.order.repository.CustomerOrderRepository;
import com.delivery.webshop.order.service.OrderTrackingService;
import com.delivery.webshop.shared.error.NotFoundException;

import org.springframework.stereotype.Service;

@Service
public class OrderTrackingServiceImpl implements OrderTrackingService {

    private final CustomerOrderRepository orders;

    public OrderTrackingServiceImpl(CustomerOrderRepository orders) {
        this.orders = orders;
    }

    @Override
    public List<CustomerOrder> getOrdersByCustomer(String customerId) {
        return orders.findTop50ByCustomerIdOrderByPlacedAtDesc(customerId);
    }

    /** Someone else's order id behaves exactly like a missing one (404), so ids can't be probed. */
    @Override
    public CustomerOrder getOrderByCustomer(String customerId, String orderId) {
        return orders.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new NotFoundException("Order", orderId));
    }
}

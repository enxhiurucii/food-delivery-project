package com.delivery.webshop.order.repository;

import java.util.List;
import java.util.Optional;

import com.delivery.webshop.order.model.CustomerOrder;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface CustomerOrderRepository extends MongoRepository<CustomerOrder, String> {

    List<CustomerOrder> findTop50ByCustomerIdOrderByPlacedAtDesc(String customerId);

    Optional<CustomerOrder> findByIdAndCustomerId(String id, String customerId);
}

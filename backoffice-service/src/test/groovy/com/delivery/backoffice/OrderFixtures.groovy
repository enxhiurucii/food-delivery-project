package com.delivery.backoffice

import com.delivery.backoffice.order.enums.FulfillmentType
import com.delivery.backoffice.order.model.DeliveryAddress
import com.delivery.backoffice.order.model.Order
import com.delivery.backoffice.order.model.OrderLine
import com.delivery.backoffice.order.model.Payment

import java.time.Instant

class OrderFixtures {

    static final Instant PLACED_AT = Instant.parse('2026-11-23T11:00:00Z')

    static Order pickupOrder(String id = 'o-1', String restaurantId = 'napoli',
                             Payment payment = cashOnCompletion()) {
        Order.place(id, restaurantId, 'customer-1', 'Alice',
                [new OrderLine('item-burger', 'Burger menu', 1000, 2)],
                250, FulfillmentType.PICKUP, null, null, payment, PLACED_AT)
    }

    static Order deliveryOrder(String id = 'o-2', String restaurantId = 'napoli',
                               Payment payment = cashOnCompletion()) {
        Order.place(id, restaurantId, 'customer-1', 'Alice',
                [new OrderLine('item-burger', 'Burger menu', 1000, 1), new OrderLine('item-cola', 'Coca-Cola', 250, 2)],
                250, FulfillmentType.DELIVERY,
                new DeliveryAddress('Rruga Myslym Shyri 1', 'Tirana', '1001', '+355690000000', 'Ring twice'),
                null, payment, PLACED_AT)
    }

    static Payment cashOnCompletion() {
        new Payment(Payment.Method.CASH, Payment.Timing.ON_COMPLETION, Payment.Status.PENDING)
    }

    static Payment paidOnline() {
        new Payment(Payment.Method.CARD, Payment.Timing.ONLINE, Payment.Status.PAID)
    }
}

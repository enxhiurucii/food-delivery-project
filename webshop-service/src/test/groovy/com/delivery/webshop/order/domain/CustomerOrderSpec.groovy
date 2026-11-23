package com.delivery.webshop.order.domain

import com.delivery.webshop.order.dto.OrderView
import com.delivery.webshop.order.enums.FulfillmentType
import com.delivery.webshop.order.enums.OrderStatus
import com.delivery.webshop.order.model.CustomerOrder
import com.delivery.webshop.order.model.DeliveryAddress
import com.delivery.webshop.order.model.OrderLine
import com.delivery.webshop.order.model.Payment
import spock.lang.Specification

import java.time.Instant

import static com.delivery.webshop.order.enums.OrderStatus.*

class CustomerOrderSpec extends Specification {

    def t0 = Instant.parse('2026-11-23T11:00:00Z')

    def "status events are applied in order and duplicates or stale events are ignored"() {
        given:
        def order = pickupOrder()

        expect:
        order.applyStatusChange(ACCEPTED, 2, Payment.Status.PENDING, t0.plusSeconds(60))
        !order.applyStatusChange(ACCEPTED, 2, Payment.Status.PENDING, t0.plusSeconds(60))     // duplicate
        order.applyStatusChange(READY_FOR_PICKUP, 4, Payment.Status.PENDING, t0.plusSeconds(900)) // 3 arrives late
        !order.applyStatusChange(PREPARING, 3, Payment.Status.PENDING, t0.plusSeconds(300))   // stale

        and:
        order.status == READY_FOR_PICKUP
        order.lastSequence == 4
        order.statusHistory*.status == [PLACED, ACCEPTED, READY_FOR_PICKUP]
    }

    def "completing an order marks a deferred payment as paid"() {
        given:
        def order = pickupOrder()

        when:
        order.applyStatusChange(PICKED_UP, 5, Payment.Status.PAID, t0.plusSeconds(1800))

        then:
        order.payment.status() == Payment.Status.PAID
    }

    def "the tracking timeline shows the pickup path with reached steps and the current one"() {
        given:
        def order = pickupOrder()
        order.applyStatusChange(ACCEPTED, 2, null, t0.plusSeconds(60))
        order.applyStatusChange(PREPARING, 3, null, t0.plusSeconds(120))

        when:
        def timeline = OrderView.from(order).timeline()

        then:
        timeline*.status() == [PLACED, ACCEPTED, PREPARING, READY_FOR_PICKUP, PICKED_UP]
        timeline*.reached() == [true, true, true, false, false]
        timeline*.current() == [false, false, true, false, false]
        timeline[1].at() == t0.plusSeconds(60)
    }

    def "a delivery order shows 'out for delivery' and a rejected order ends with REJECTED"() {
        given:
        def delivery = CustomerOrder.place('o-2', 'c-1', 'Alice', 'napoli', 'Pizzeria Napoli',
                [new OrderLine('i', 'Pizza', 800, 2)], 250, FulfillmentType.DELIVERY,
                new DeliveryAddress('Street 1', 'Tirana', null, '+355', null), null,
                new Payment(Payment.Method.CASH, Payment.Timing.ON_COMPLETION, Payment.Status.PENDING, null),
                t0, t0.plusSeconds(1200))
        def rejected = pickupOrder()
        rejected.applyStatusChange(REJECTED, 2, null, t0.plusSeconds(30))

        expect:
        OrderView.from(delivery).timeline()*.status() == [PLACED, ACCEPTED, PREPARING, OUT_FOR_DELIVERY, DELIVERED]
        delivery.totalCents == 1850
        OrderView.from(rejected).timeline()*.status() == [PLACED, REJECTED]
        OrderView.from(rejected).finished()
    }

    private CustomerOrder pickupOrder() {
        CustomerOrder.place('o-1', 'c-1', 'Alice', 'napoli', 'Pizzeria Napoli',
                [new OrderLine('burger', 'Burger menu', 1000, 2)], 250, FulfillmentType.PICKUP, null, null,
                new Payment(Payment.Method.CASH, Payment.Timing.ON_COMPLETION, Payment.Status.PENDING, null),
                t0, t0.plusSeconds(1200))
    }
}

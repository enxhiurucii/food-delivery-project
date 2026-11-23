package com.delivery.backoffice.order.domain

import com.delivery.backoffice.OrderFixtures
import com.delivery.backoffice.order.exception.InvalidStatusTransitionException
import com.delivery.backoffice.order.model.Order
import com.delivery.backoffice.order.model.OrderLine
import com.delivery.backoffice.order.model.Payment
import spock.lang.Specification

import java.time.Instant

import static com.delivery.backoffice.order.enums.FulfillmentType.*
import static com.delivery.backoffice.order.enums.OrderStatus.*

class OrderSpec extends Specification {

    def t1 = Instant.parse('2026-11-23T11:05:00Z')
    def t2 = Instant.parse('2026-11-23T11:20:00Z')
    def t3 = Instant.parse('2026-11-23T11:40:00Z')
    def t4 = Instant.parse('2026-11-23T12:05:00Z')

    def "a new order starts as PLACED with totals computed from the line snapshots"() {
        when:
        def order = OrderFixtures.deliveryOrder()

        then:
        order.status == PLACED
        order.subtotalCents == 1000 + 2 * 250
        order.deliveryFeeCents == 250
        order.totalCents == 1750
        order.statusHistory*.status == [PLACED]
        order.closedAt == null
    }

    def "pickup orders never carry a delivery fee or address"() {
        when:
        def order = OrderFixtures.pickupOrder()

        then:
        order.deliveryFeeCents == 0
        order.deliveryAddress == null
        order.totalCents == 2000
    }

    def "a delivery order without an address is rejected"() {
        when:
        Order.place('o-x', 'napoli', 'c', 'Alice', [new OrderLine('i', 'Pizza', 800, 1)], 250,
                DELIVERY, null, null, OrderFixtures.cashOnCompletion(), t1)

        then:
        thrown(IllegalArgumentException)
    }

    def "walking a delivery order through its lifecycle builds the timeline, closes it and settles cash payment"() {
        given:
        def order = OrderFixtures.deliveryOrder()

        when:
        order.changeStatus(ACCEPTED, t1)
        order.changeStatus(PREPARING, t2)
        order.changeStatus(READY_FOR_PICKUP, t3)
        order.changeStatus(OUT_FOR_DELIVERY, t3)
        order.changeStatus(DELIVERED, t4)

        then:
        order.status == DELIVERED
        order.statusHistory*.status == [PLACED, ACCEPTED, PREPARING, READY_FOR_PICKUP, OUT_FOR_DELIVERY, DELIVERED]
        order.statusSequence() == 6
        order.closedAt == t4
        order.payment.status() == Payment.Status.PAID
    }

    def "a rejected order is closed but a deferred payment stays unpaid"() {
        given:
        def order = OrderFixtures.pickupOrder()

        when:
        order.changeStatus(REJECTED, t1)

        then:
        order.closedAt == t1
        order.payment.status() == Payment.Status.PENDING
    }

    def "an invalid transition is refused and leaves the order untouched"() {
        given:
        def order = OrderFixtures.pickupOrder()

        when:
        order.changeStatus(OUT_FOR_DELIVERY, t1)

        then:
        def ex = thrown(InvalidStatusTransitionException)
        ex.message.contains('PLACED')
        order.status == PLACED
        order.statusHistory.size() == 1
    }
}

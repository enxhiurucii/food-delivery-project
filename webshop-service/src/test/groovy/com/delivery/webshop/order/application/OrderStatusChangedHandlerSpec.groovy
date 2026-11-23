package com.delivery.webshop.order.application

import com.delivery.webshop.checkout.service.PaymentProcessor
import com.delivery.webshop.order.enums.FulfillmentType
import com.delivery.webshop.order.enums.OrderStatus
import com.delivery.webshop.order.listener.OrderStatusChangedHandler
import com.delivery.webshop.order.model.CustomerOrder
import com.delivery.webshop.order.model.OrderLine
import com.delivery.webshop.order.model.Payment
import com.delivery.webshop.order.repository.CustomerOrderRepository
import com.delivery.webshop.shared.messaging.event.OrderStatusChangedEvent
import com.delivery.webshop.shared.sse.SseHub
import spock.lang.Specification
import spock.lang.Subject

import java.time.Instant

class OrderStatusChangedHandlerSpec extends Specification {

    def t0 = Instant.parse('2026-11-23T11:00:00Z')
    CustomerOrderRepository orders = Mock()
    SseHub sseHub = Mock()
    PaymentProcessor paymentProcessor = Mock()

    @Subject
    OrderStatusChangedHandler handler = new OrderStatusChangedHandler(orders, sseHub, paymentProcessor)

    def order = CustomerOrder.place('o-1', 'c-1', 'Alice', 'napoli', 'Pizzeria Napoli',
            [new OrderLine('burger', 'Burger menu', 1000, 1)], 0, FulfillmentType.PICKUP, null, null,
            new Payment(Payment.Method.CASH, Payment.Timing.ON_COMPLETION, Payment.Status.PENDING, null),
            t0, t0.plusSeconds(1200))

    def cardOrder = CustomerOrder.place('o-2', 'c-1', 'Alice', 'napoli', 'Pizzeria Napoli',
            [new OrderLine('burger', 'Burger menu', 1000, 1)], 0, FulfillmentType.PICKUP, null, null,
            new Payment(Payment.Method.CARD, Payment.Timing.ONLINE, Payment.Status.PAID, 'mock_xyz'),
            t0, t0.plusSeconds(1200))

    def "a new status updates the projection and pushes the timeline to the tracking page"() {
        given:
        orders.findById('o-1') >> Optional.of(order)

        when:
        handler.handle(event(OrderStatus.ACCEPTED, 2))

        then:
        1 * orders.save({ it.status == OrderStatus.ACCEPTED }) >> { CustomerOrder o -> o }
        1 * sseHub.publish('order:o-1', 'order-updated', { it.status() == OrderStatus.ACCEPTED })
        0 * paymentProcessor._
    }

    def "rejecting an order paid by card online triggers a refund and marks payment REFUNDED"() {
        given:
        orders.findById('o-2') >> Optional.of(cardOrder)

        when:
        handler.handle(new OrderStatusChangedEvent(UUID.randomUUID(), t0, 'o-2', 'napoli', 'REJECTED', 2, 'PAID', t0.plusSeconds(60)))

        then:
        1 * paymentProcessor.refund('mock_xyz')
        1 * orders.save({ it.payment.status() == Payment.Status.REFUNDED }) >> { CustomerOrder o -> o }
        1 * sseHub.publish('order:o-2', 'order-updated', { it.paymentStatus() == 'REFUNDED' })
    }

    def "rejecting a cash order does not trigger a refund"() {
        given:
        orders.findById('o-1') >> Optional.of(order)

        when:
        handler.handle(event(OrderStatus.REJECTED, 2))

        then:
        0 * paymentProcessor._
        1 * orders.save(_) >> { CustomerOrder o -> o }
    }

    def "a duplicate event changes nothing and pushes nothing"() {
        given:
        orders.findById('o-1') >> Optional.of(order)

        when:
        handler.handle(event(OrderStatus.PLACED, 1))

        then:
        0 * orders.save(_)
        0 * sseHub._
    }

    def "an event for an unknown order is dropped instead of retried forever"() {
        given:
        orders.findById('ghost') >> Optional.empty()

        when:
        handler.handle(new OrderStatusChangedEvent(UUID.randomUUID(), t0, 'ghost', 'napoli', 'ACCEPTED', 2, 'PENDING', t0))

        then:
        noExceptionThrown()
        0 * sseHub._
    }

    private OrderStatusChangedEvent event(OrderStatus status, int sequence) {
        new OrderStatusChangedEvent(UUID.randomUUID(), t0, 'o-1', 'napoli', status.name(), sequence, 'PENDING',
                t0.plusSeconds(60))
    }
}

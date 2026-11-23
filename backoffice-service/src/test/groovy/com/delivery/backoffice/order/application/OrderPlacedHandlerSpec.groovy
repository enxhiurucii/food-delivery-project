package com.delivery.backoffice.order.application

import com.delivery.backoffice.order.enums.FulfillmentType
import com.delivery.backoffice.order.enums.OrderStatus
import com.delivery.backoffice.order.listener.OrderPlacedHandler
import com.delivery.backoffice.order.model.Order
import com.delivery.backoffice.order.model.OrderChanged
import com.delivery.backoffice.order.model.Payment
import com.delivery.backoffice.order.repository.OrderRepository
import com.delivery.backoffice.shared.messaging.event.OrderPlacedEvent
import org.springframework.context.ApplicationEventPublisher
import spock.lang.Specification
import spock.lang.Subject

import java.time.Instant

class OrderPlacedHandlerSpec extends Specification {

    OrderRepository repository = Mock()
    ApplicationEventPublisher events = Mock()

    @Subject
    OrderPlacedHandler handler = new OrderPlacedHandler(repository, events)

    def event = new OrderPlacedEvent(UUID.randomUUID(), Instant.parse('2026-11-23T11:00:00Z'), 'o-9', 'napoli',
            'customer-1', 'Alice', [new OrderPlacedEvent.Line('item-1', 'Burger menu', 1000, 2)],
            2000, 250, 2250, 'DELIVERY',
            new OrderPlacedEvent.Address('Rruga 1', 'Tirana', '1001', '+355', null), 'No onions',
            'CARD', 'ONLINE', 'PAID')

    def "a new order.placed event creates a PLACED kitchen order and notifies the dashboard"() {
        given:
        repository.existsById('o-9') >> false

        when:
        handler.handle(event)

        then:
        1 * repository.save({ Order o ->
            o.id == 'o-9' &&
                    o.status == OrderStatus.PLACED &&
                    o.fulfillmentType == FulfillmentType.DELIVERY &&
                    o.totalCents == 2250 &&
                    o.payment == new Payment(Payment.Method.CARD, Payment.Timing.ONLINE, Payment.Status.PAID) &&
                    o.deliveryAddress.city() == 'Tirana'
        }) >> { Order o -> o }
        1 * events.publishEvent({ it instanceof OrderChanged && it.type() == OrderChanged.Type.PLACED })
    }

    def "a redelivered event is ignored (idempotent consumer)"() {
        given:
        repository.existsById('o-9') >> true

        when:
        handler.handle(event)

        then:
        0 * repository.save(_)
        0 * events._
    }
}

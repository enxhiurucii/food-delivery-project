package com.delivery.backoffice.order.application

import com.delivery.backoffice.OrderFixtures
import com.delivery.backoffice.order.enums.OrderStatus
import com.delivery.backoffice.order.exception.InvalidStatusTransitionException
import com.delivery.backoffice.order.model.Order
import com.delivery.backoffice.order.model.OrderChanged
import com.delivery.backoffice.order.repository.OrderRepository
import com.delivery.backoffice.order.service.OrderService
import com.delivery.backoffice.order.service.impl.OrderServiceImpl
import com.delivery.backoffice.shared.error.NotFoundException
import com.delivery.backoffice.shared.messaging.RoutingKeys
import com.delivery.backoffice.shared.messaging.event.OrderStatusChangedEvent
import com.delivery.backoffice.shared.messaging.outbox.Outbox
import org.springframework.context.ApplicationEventPublisher
import spock.lang.Specification
import spock.lang.Subject

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class OrderServiceSpec extends Specification {

    OrderRepository repository = Mock()
    Outbox outbox = Mock()
    ApplicationEventPublisher events = Mock()
    Clock clock = Clock.fixed(Instant.parse('2026-11-23T12:00:00Z'), ZoneOffset.UTC)

    @Subject
    OrderService service = new OrderServiceImpl(repository, outbox, events, clock)

    def "accepting an order saves it, enqueues the integration event and notifies the dashboard"() {
        given:
        repository.findByIdAndRestaurantId('o-1', 'napoli') >> Optional.of(OrderFixtures.pickupOrder('o-1'))
        repository.save(_ as Order) >> { Order order -> order }

        when:
        def result = service.updateOrderStatus('napoli', 'o-1', OrderStatus.ACCEPTED)

        then:
        result.status == OrderStatus.ACCEPTED
        1 * outbox.enqueue(RoutingKeys.ORDER_STATUS_CHANGED, _ as UUID, { OrderStatusChangedEvent e ->
            e.orderId() == 'o-1' &&
                    e.restaurantId() == 'napoli' &&
                    e.status() == 'ACCEPTED' &&
                    e.sequence() == 2 &&
                    e.paymentStatus() == 'PENDING' &&
                    e.changedAt() == clock.instant()
        })
        1 * events.publishEvent({ it instanceof OrderChanged && it.type() == OrderChanged.Type.STATUS_CHANGED })
    }

    def "an order of another restaurant is reported as not found"() {
        given:
        repository.findByIdAndRestaurantId('o-1', 'sushi-zen') >> Optional.empty()

        when:
        service.updateOrderStatus('sushi-zen', 'o-1', OrderStatus.ACCEPTED)

        then:
        thrown(NotFoundException)
        0 * outbox._
    }

    def "an invalid transition publishes nothing"() {
        given:
        repository.findByIdAndRestaurantId('o-1', 'napoli') >> Optional.of(OrderFixtures.pickupOrder('o-1'))

        when:
        service.updateOrderStatus('napoli', 'o-1', OrderStatus.DELIVERED)

        then:
        thrown(InvalidStatusTransitionException)
        0 * repository.save(_)
        0 * outbox._
        0 * events._
    }

    def "without a status filter the board shows active orders only"() {
        when:
        service.getOrdersByRestaurant('napoli', null)

        then:
        1 * repository.findByRestaurantIdAndStatusInOrderByPlacedAtDesc('napoli', OrderService.ACTIVE_STATUSES) >> []
    }
}

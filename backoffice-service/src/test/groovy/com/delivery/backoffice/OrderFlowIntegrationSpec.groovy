package com.delivery.backoffice

import com.delivery.backoffice.order.enums.OrderStatus
import com.delivery.backoffice.order.repository.OrderRepository
import com.delivery.backoffice.order.service.OrderService
import com.delivery.backoffice.shared.messaging.RoutingKeys
import com.delivery.backoffice.shared.messaging.event.OrderPlacedEvent
import com.delivery.backoffice.shared.messaging.outbox.OutboxRepository
import org.springframework.amqp.core.MessageBuilder
import org.springframework.amqp.core.MessageProperties
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.testcontainers.DockerClientFactory
import spock.lang.Requires
import spock.lang.Specification
import spock.util.concurrent.PollingConditions
import tools.jackson.databind.json.JsonMapper

import java.time.Instant

/**
 * End-to-end inside the service with real infrastructure:
 * RabbitMQ "order.placed" -> listener -> MongoDB, then status change -> outbox (same transaction) -> RabbitMQ.
 * Skipped automatically when Docker is not available.
 */
@Requires({ DockerClientFactory.instance().isDockerAvailable() })
@SpringBootTest(properties = ['spring.rabbitmq.virtual-host=/', 'app.seed.enabled=false', 'app.outbox.poll-interval-ms=200'])
@Import(TestcontainersConfig)
class OrderFlowIntegrationSpec extends Specification {

    @Autowired
    RabbitTemplate rabbitTemplate
    @Autowired
    JsonMapper jsonMapper
    @Autowired
    OrderRepository orders
    @Autowired
    OrderService orderService
    @Autowired
    OutboxRepository outbox

    def conditions = new PollingConditions(timeout: 90, initialDelay: 0.5)

    def "an order.placed event becomes exactly one kitchen order and its status change is relayed through the outbox"() {
        given:
        def event = new OrderPlacedEvent(UUID.randomUUID(), Instant.now(), 'order-it-1', 'napoli', 'customer-1',
                'Alice', [new OrderPlacedEvent.Line('item-1', 'Burger menu', 1000, 2)], 2000, 0, 2000, 'PICKUP',
                null, null, 'CASH', 'ON_COMPLETION', 'PENDING')
        def message = MessageBuilder.withBody(jsonMapper.writeValueAsBytes(event))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON).build()

        when: 'the broker delivers the same event twice'
        2.times { rabbitTemplate.send('orders.events', RoutingKeys.ORDER_PLACED, message) }

        then:
        conditions.eventually {
            assert orders.findById('order-it-1').map { it.status }.orElse(null) == OrderStatus.PLACED
        }
        orders.findAll().count { it.id == 'order-it-1' } == 1

        when:
        orderService.updateOrderStatus('napoli', 'order-it-1', OrderStatus.ACCEPTED)

        then:
        conditions.eventually {
            def entries = outbox.findAll()
            assert entries.size() == 1
            assert entries[0].routingKey == RoutingKeys.ORDER_STATUS_CHANGED
            assert entries[0].publishedAt != null
        }
    }
}

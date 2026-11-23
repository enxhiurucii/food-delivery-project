package com.delivery.webshop

import com.delivery.webshop.order.enums.FulfillmentType
import com.delivery.webshop.order.enums.OrderStatus
import com.delivery.webshop.order.model.CustomerOrder
import com.delivery.webshop.order.model.OrderLine
import com.delivery.webshop.order.model.Payment
import com.delivery.webshop.order.repository.CustomerOrderRepository
import com.delivery.webshop.shared.messaging.RoutingKeys
import com.delivery.webshop.shared.messaging.event.OrderStatusChangedEvent
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

/** RabbitMQ "order.status.changed" -> listener -> MongoDB projection, with real infrastructure. Needs Docker. */
@Requires({ DockerClientFactory.instance().isDockerAvailable() })
@SpringBootTest(properties = ['spring.rabbitmq.virtual-host=/'])
@Import(TestcontainersConfig)
class OrderStatusFlowIntegrationSpec extends Specification {

    @Autowired
    RabbitTemplate rabbitTemplate
    @Autowired
    JsonMapper jsonMapper
    @Autowired
    CustomerOrderRepository orders

    def "status events from the back office update the customer's order, ignoring out-of-order duplicates"() {
        given:
        def placedAt = Instant.parse('2026-11-23T11:00:00Z')
        orders.save(CustomerOrder.place('order-it-1', 'customer-1', 'Alice', 'napoli', 'Pizzeria Napoli',
                [new OrderLine('burger', 'Burger menu', 1000, 1)], 0, FulfillmentType.PICKUP, null, null,
                new Payment(Payment.Method.CASH, Payment.Timing.ON_COMPLETION, Payment.Status.PENDING, null),
                placedAt, placedAt.plusSeconds(1200)))

        when:
        publish('ACCEPTED', 2)
        publish('PREPARING', 3)
        publish('ACCEPTED', 2) // redelivered late

        then:
        new PollingConditions(timeout: 15).eventually {
            def order = orders.findById('order-it-1').orElseThrow()
            assert order.status == OrderStatus.PREPARING
            assert order.statusHistory*.status == [OrderStatus.PLACED, OrderStatus.ACCEPTED, OrderStatus.PREPARING]
        }
    }

    private void publish(String status, int sequence) {
        def event = new OrderStatusChangedEvent(UUID.randomUUID(), Instant.now(), 'order-it-1', 'napoli', status,
                sequence, 'PENDING', Instant.now())
        rabbitTemplate.send('orders.events', RoutingKeys.ORDER_STATUS_CHANGED,
                MessageBuilder.withBody(jsonMapper.writeValueAsBytes(event))
                        .setContentType(MessageProperties.CONTENT_TYPE_JSON).build())
    }
}

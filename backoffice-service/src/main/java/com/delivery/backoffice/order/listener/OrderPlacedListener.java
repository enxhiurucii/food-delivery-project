package com.delivery.backoffice.order.listener;

import com.delivery.backoffice.shared.messaging.event.OrderPlacedEvent;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Inbound adapter: only converts the AMQP message and delegates. Failures are retried (see
 * spring.rabbitmq.listener.simple.retry) and then dead-lettered to backoffice.order-placed.dlq.
 */
@Component
public class OrderPlacedListener {

    private final OrderPlacedHandler handler;
    private final JsonMapper jsonMapper;

    public OrderPlacedListener(OrderPlacedHandler handler, JsonMapper jsonMapper) {
        this.handler = handler;
        this.jsonMapper = jsonMapper;
    }

    @RabbitListener(queues = "${app.messaging.order-placed-queue}")
    public void onOrderPlaced(Message message) {
        handler.handle(jsonMapper.readValue(message.getBody(), OrderPlacedEvent.class));
    }
}

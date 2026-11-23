package com.delivery.webshop.order.listener;

import com.delivery.webshop.shared.messaging.event.OrderStatusChangedEvent;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Inbound adapter for "order.status.changed". Retried, then dead-lettered to webshop.order-status.dlq. */
@Component
public class OrderStatusChangedListener {

    private final OrderStatusChangedHandler handler;
    private final JsonMapper jsonMapper;

    public OrderStatusChangedListener(OrderStatusChangedHandler handler, JsonMapper jsonMapper) {
        this.handler = handler;
        this.jsonMapper = jsonMapper;
    }

    @RabbitListener(queues = "${app.messaging.order-status-queue}")
    public void onStatusChanged(Message message) {
        handler.handle(jsonMapper.readValue(message.getBody(), OrderStatusChangedEvent.class));
    }
}

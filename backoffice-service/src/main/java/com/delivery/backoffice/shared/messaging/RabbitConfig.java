package com.delivery.backoffice.shared.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topology owned by the back office:
 * <pre>
 * orders.events (topic) --order.placed--> backoffice.order-placed --(after 3 failed attempts)--> orders.events.dlx
 *                                                                                                  --> backoffice.order-placed.dlq
 * </pre>
 * The exchange declarations are idempotent; the webshop declares the same exchanges with the same arguments.
 * Messages are plain JSON (no Java type headers), so the two services stay decoupled.
 */
@Configuration(proxyBeanMethods = false)
public class RabbitConfig {

    @Bean
    TopicExchange ordersExchange(MessagingProperties props) {
        return ExchangeBuilder.topicExchange(props.exchange()).durable(true).build();
    }

    @Bean
    DirectExchange deadLetterExchange(MessagingProperties props) {
        return ExchangeBuilder.directExchange(props.deadLetterExchange()).durable(true).build();
    }

    @Bean
    Queue orderPlacedQueue(MessagingProperties props) {
        return QueueBuilder.durable(props.orderPlacedQueue())
                .deadLetterExchange(props.deadLetterExchange())
                .deadLetterRoutingKey(props.orderPlacedQueue())
                .build();
    }

    @Bean
    Queue orderPlacedDeadLetterQueue(MessagingProperties props) {
        return QueueBuilder.durable(props.orderPlacedQueue() + ".dlq").build();
    }

    @Bean
    Binding orderPlacedBinding(Queue orderPlacedQueue, TopicExchange ordersExchange) {
        return BindingBuilder.bind(orderPlacedQueue).to(ordersExchange).with(RoutingKeys.ORDER_PLACED);
    }

    @Bean
    Binding orderPlacedDeadLetterBinding(Queue orderPlacedDeadLetterQueue, DirectExchange deadLetterExchange,
                                         MessagingProperties props) {
        return BindingBuilder.bind(orderPlacedDeadLetterQueue).to(deadLetterExchange).with(props.orderPlacedQueue());
    }
}

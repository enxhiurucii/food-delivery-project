package com.delivery.webshop.shared.messaging;

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
 * Topology owned by the webshop:
 * <pre>
 * orders.events (topic) --order.status.changed--> webshop.order-status --(after retries)--> orders.events.dlx
 *                                                                                            --> webshop.order-status.dlq
 * </pre>
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
    Queue orderStatusQueue(MessagingProperties props) {
        return QueueBuilder.durable(props.orderStatusQueue())
                .deadLetterExchange(props.deadLetterExchange())
                .deadLetterRoutingKey(props.orderStatusQueue())
                .build();
    }

    @Bean
    Queue orderStatusDeadLetterQueue(MessagingProperties props) {
        return QueueBuilder.durable(props.orderStatusQueue() + ".dlq").build();
    }

    @Bean
    Binding orderStatusBinding(Queue orderStatusQueue, TopicExchange ordersExchange) {
        return BindingBuilder.bind(orderStatusQueue).to(ordersExchange).with(RoutingKeys.ORDER_STATUS_CHANGED);
    }

    @Bean
    Binding orderStatusDeadLetterBinding(Queue orderStatusDeadLetterQueue, DirectExchange deadLetterExchange,
                                         MessagingProperties props) {
        return BindingBuilder.bind(orderStatusDeadLetterQueue).to(deadLetterExchange).with(props.orderStatusQueue());
    }
}

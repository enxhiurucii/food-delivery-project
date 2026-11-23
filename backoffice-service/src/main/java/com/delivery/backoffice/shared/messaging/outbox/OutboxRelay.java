package com.delivery.backoffice.shared.messaging.outbox;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;

import com.delivery.backoffice.shared.messaging.MessagingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Publishes pending outbox events with publisher confirms. Delivery is at-least-once (a crash after
 * the broker confirm but before marking the row as published re-sends it), so every consumer is idempotent.
 *
 * <p>The AMQP {@code user-id} property is set to the connection's user. RabbitMQ rejects the publish if it
 * does not match ("validated user-id"), so consumers can trust which service sent the event.
 *
 * <p>Note: with several instances, add a lock (e.g. ShedLock) or claim rows with findAndModify.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final long CONFIRM_TIMEOUT_MS = 5_000;

    private final OutboxRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties props;
    private final Clock clock;
    private final int batchSize;

    public OutboxRelay(OutboxRepository repository, RabbitTemplate rabbitTemplate, MessagingProperties props,
                       Clock clock, @Value("${app.outbox.batch-size:50}") int batchSize) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
        this.clock = clock;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:1000}")
    public void publishPending() {
        List<OutboxEvent> pending = repository.findByPublishedAtIsNullOrderByCreatedAtAsc(PageRequest.of(0, batchSize));
        for (OutboxEvent event : pending) {
            try {
                publish(event);
                event.markPublished(clock.instant());
            } catch (RuntimeException ex) {
                log.warn("Publishing outbox event {} failed (attempt {}): {}", event.getEventId(),
                        event.getAttempts() + 1, ex.getMessage());
                event.markFailed(ex.getMessage());
                repository.save(event);
                return; // keep ordering: retry the oldest event on the next tick
            }
            repository.save(event);
        }
    }

    private void publish(OutboxEvent event) {
        Message message = MessageBuilder.withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setContentEncoding(StandardCharsets.UTF_8.name())
                .setMessageId(event.getEventId())
                .setType(event.getEventType())
                .setUserId(rabbitTemplate.getConnectionFactory().getUsername())
                .build();
        rabbitTemplate.invoke(operations -> {
            operations.send(props.exchange(), event.getRoutingKey(), message);
            operations.waitForConfirmsOrDie(CONFIRM_TIMEOUT_MS);
            return null;
        });
    }
}

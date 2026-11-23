package com.delivery.backoffice.shared.messaging.outbox;

import java.time.Instant;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Transactional outbox entry. Written in the SAME MongoDB transaction as the business change, then
 * published to RabbitMQ by {@link OutboxRelay}. This avoids the "saved but never published" (or
 * "published but rolled back") problem of publishing directly from the service.
 */
@Getter
@Document("outbox")
public class OutboxEvent {

    @Id
    private String id;
    private String eventId;
    private String eventType;
    private String routingKey;
    private String payload;
    private Instant createdAt;
    @Indexed
    private Instant publishedAt;
    private int attempts;
    private String lastError;

    protected OutboxEvent() {
    }

    public OutboxEvent(String eventId, String eventType, String routingKey, String payload, Instant createdAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.routingKey = routingKey;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public void markPublished(Instant at) {
        this.publishedAt = at;
        this.lastError = null;
    }

    public void markFailed(String error) {
        this.attempts++;
        this.lastError = error;
    }
}

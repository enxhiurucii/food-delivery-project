package com.delivery.webshop.shared.messaging.outbox;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Call from inside a @Transactional service method so the event commits together with the state change. */
@Component
public class Outbox {

    private final OutboxRepository repository;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    public Outbox(OutboxRepository repository, JsonMapper jsonMapper, Clock clock) {
        this.repository = repository;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
    }

    public void enqueue(String routingKey, UUID eventId, Object event) {
        String payload = jsonMapper.writeValueAsString(event);
        repository.save(new OutboxEvent(eventId.toString(), event.getClass().getSimpleName(), routingKey, payload,
                clock.instant()));
    }
}

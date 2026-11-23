package com.delivery.backoffice.shared.sse;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * In-memory registry of open Server-Sent-Event connections, grouped by topic (e.g. "restaurant:napoli").
 *
 * <p>Scaling note: with several instances each instance only knows its own connections. The fix is to
 * let every instance consume order events from its own exclusive, auto-delete queue (fan-out) and
 * push to its local emitters.
 */
@Component
public class SseHub {

    private static final long EMITTER_TIMEOUT_MS = Duration.ofMinutes(30).toMillis();

    private final Map<String, Set<SseEmitter>> emittersByTopic = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String topic) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        emittersByTopic.computeIfAbsent(topic, key -> ConcurrentHashMap.newKeySet()).add(emitter);
        Runnable remove = () -> remove(topic, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(error -> remove.run());
        send(topic, emitter, SseEmitter.event().name("connected").data(Map.of("topic", topic), MediaType.APPLICATION_JSON));
        return emitter;
    }

    public void publish(String topic, String eventName, Object payload) {
        for (SseEmitter emitter : emittersByTopic.getOrDefault(topic, Set.of())) {
            send(topic, emitter, SseEmitter.event().name(eventName).data(payload, MediaType.APPLICATION_JSON));
        }
    }

    /** Keeps idle connections open through proxies (nginx closes idle upstreams after proxy_read_timeout). */
    @Scheduled(fixedRate = 25_000)
    public void heartbeat() {
        emittersByTopic.forEach((topic, emitters) ->
                emitters.forEach(emitter -> send(topic, emitter, SseEmitter.event().comment("keep-alive"))));
    }

    int connectionCount(String topic) {
        return emittersByTopic.getOrDefault(topic, Set.of()).size();
    }

    private void send(String topic, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException ex) {
            remove(topic, emitter);
        }
    }

    private void remove(String topic, SseEmitter emitter) {
        Set<SseEmitter> emitters = emittersByTopic.get(topic);
        if (emitters != null) {
            emitters.remove(emitter);
        }
    }
}

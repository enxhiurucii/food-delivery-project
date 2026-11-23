package com.delivery.backoffice.shared.sse;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * One-time tickets for SSE. The browser's EventSource cannot send an Authorization header, and a real
 * JWT in the URL would leak into logs, proxies and browser history. Instead the SPA calls an
 * authenticated POST endpoint to get a random ticket valid for a few seconds and usable exactly once,
 * bound to one topic, and opens {@code EventSource(".../stream?ticket=...")}.
 *
 * <p>Single-instance store; with several instances move it to Redis (SET with EX + GETDEL).
 */
@Component
public class SseTicketService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();
    private final Clock clock;
    private final Duration ttl;

    public SseTicketService(Clock clock, @Value("${app.sse.ticket-ttl:30s}") Duration ttl) {
        this.clock = clock;
        this.ttl = ttl;
    }

    public IssuedTicket issue(String topic) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tickets.put(value, new Ticket(topic, clock.instant().plus(ttl)));
        return new IssuedTicket(value, ttl.toSeconds());
    }

    /** Consumes the ticket. Returns empty if unknown, expired, already used or issued for another topic. */
    public Optional<String> redeem(String value, String expectedTopic) {
        if (value == null) {
            return Optional.empty();
        }
        Ticket ticket = tickets.remove(value);
        if (ticket == null || ticket.expiresAt().isBefore(clock.instant()) || !ticket.topic().equals(expectedTopic)) {
            return Optional.empty();
        }
        return Optional.of(ticket.topic());
    }

    @Scheduled(fixedRate = 60_000)
    public void purgeExpired() {
        Instant now = clock.instant();
        tickets.values().removeIf(ticket -> ticket.expiresAt().isBefore(now));
    }

    private record Ticket(String topic, Instant expiresAt) {
    }

    public record IssuedTicket(String ticket, long expiresInSeconds) {
    }
}

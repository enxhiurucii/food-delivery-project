package com.delivery.backoffice.order.controller;

import java.util.List;
import java.util.Set;

import com.delivery.backoffice.order.dto.ChangeStatusRequest;
import com.delivery.backoffice.order.dto.OrderResponse;
import com.delivery.backoffice.order.enums.OrderStatus;
import com.delivery.backoffice.order.listener.OrderSseNotifier;
import com.delivery.backoffice.order.service.OrderService;
import com.delivery.backoffice.shared.sse.SseHub;
import com.delivery.backoffice.shared.sse.SseTicketService;
import com.delivery.backoffice.shared.sse.SseTicketService.IssuedTicket;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** The restaurant's order board. */
@RestController
@RequestMapping("/api/restaurants/{restaurantId}/orders")
public class OrderController {

    private static final String TEAM = "@tenantGuard.canAccess(#restaurantId, authentication)";

    private final OrderService service;
    private final SseHub sseHub;
    private final SseTicketService tickets;

    public OrderController(OrderService service, SseHub sseHub, SseTicketService tickets) {
        this.service = service;
        this.sseHub = sseHub;
        this.tickets = tickets;
    }

    /** @param status optional filter, e.g. {@code ?status=PLACED,ACCEPTED}; defaults to all active orders. */
    @GetMapping
    @PreAuthorize(TEAM)
    List<OrderResponse> getOrdersByRestaurant(@PathVariable String restaurantId,
                             @RequestParam(required = false) Set<OrderStatus> status) {
        return service.getOrdersByRestaurant(restaurantId, status).stream().map(OrderResponse::from).toList();
    }

    @GetMapping("/{orderId}")
    @PreAuthorize(TEAM)
    OrderResponse getOrderById(@PathVariable String restaurantId, @PathVariable String orderId) {
        return OrderResponse.from(service.getOrderById(restaurantId, orderId));
    }

    @PostMapping("/{orderId}/status")
    @PreAuthorize(TEAM)
    OrderResponse updateOrderStatus(@PathVariable String restaurantId, @PathVariable String orderId,
                               @Valid @RequestBody ChangeStatusRequest request) {
        return OrderResponse.from(service.updateOrderStatus(restaurantId, orderId, request.status()));
    }

    /** Step 1 of the SSE handshake (authenticated with the bearer token). */
    @PostMapping("/stream-ticket")
    @PreAuthorize(TEAM)
    IssuedTicket issueStreamTicket(@PathVariable String restaurantId) {
        return tickets.issue(OrderSseNotifier.topicFor(restaurantId));
    }

    /** Step 2: {@code new EventSource('/api/restaurants/{id}/orders/stream?ticket=...')}. */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter streamOrders(@PathVariable String restaurantId, @RequestParam String ticket) {
        String topic = OrderSseNotifier.topicFor(restaurantId);
        tickets.redeem(ticket, topic)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired ticket"));
        return sseHub.subscribe(topic);
    }
}

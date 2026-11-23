package com.delivery.webshop.order.controller;

import java.util.List;

import com.delivery.webshop.order.dto.OrderView;
import com.delivery.webshop.order.listener.OrderStatusChangedHandler;
import com.delivery.webshop.order.service.OrderTrackingService;
import com.delivery.webshop.shared.security.CurrentCustomer;
import com.delivery.webshop.shared.sse.SseHub;
import com.delivery.webshop.shared.sse.SseTicketService;
import com.delivery.webshop.shared.sse.SseTicketService.IssuedTicket;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Order history and the live tracking page. */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderTrackingService tracking;
    private final SseHub sseHub;
    private final SseTicketService tickets;

    public OrderController(OrderTrackingService tracking, SseHub sseHub, SseTicketService tickets) {
        this.tracking = tracking;
        this.sseHub = sseHub;
        this.tickets = tickets;
    }

    @GetMapping
    public List<OrderView> getOrdersByCustomer(@AuthenticationPrincipal Jwt jwt) {
        return tracking.getOrdersByCustomer(CurrentCustomer.from(jwt).id()).stream().map(OrderView::from).toList();
    }

    @GetMapping("/{orderId}")
    public OrderView getOrderByCustomer(@AuthenticationPrincipal Jwt jwt, @PathVariable String orderId) {
        return OrderView.from(tracking.getOrderByCustomer(CurrentCustomer.from(jwt).id(), orderId));
    }

    /** SSE step 1: authenticated with the bearer token; also proves the order belongs to the caller. */
    @PostMapping("/{orderId}/stream-ticket")
    public IssuedTicket issueStreamTicket(@AuthenticationPrincipal Jwt jwt, @PathVariable String orderId) {
        tracking.getOrderByCustomer(CurrentCustomer.from(jwt).id(), orderId);
        return tickets.issue(OrderStatusChangedHandler.topicFor(orderId));
    }

    /** SSE step 2: {@code new EventSource('/api/orders/{id}/stream?ticket=...')}; events named "order-updated". */
    @GetMapping(path = "/{orderId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamOrder(@PathVariable String orderId, @RequestParam String ticket) {
        String topic = OrderStatusChangedHandler.topicFor(orderId);
        tickets.redeem(ticket, topic)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired ticket"));
        return sseHub.subscribe(topic);
    }
}

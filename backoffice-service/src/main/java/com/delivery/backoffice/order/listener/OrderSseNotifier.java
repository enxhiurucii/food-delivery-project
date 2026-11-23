package com.delivery.backoffice.order.listener;

import com.delivery.backoffice.order.dto.OrderResponse;
import com.delivery.backoffice.order.model.OrderChanged;
import com.delivery.backoffice.shared.sse.SseHub;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Pushes order changes to every open restaurant dashboard, after the transaction committed.
 * Event names: "order-placed" (show a "New order!" notification) and "order-updated".
 */
@Component
public class OrderSseNotifier {

    private final SseHub sseHub;

    public OrderSseNotifier(SseHub sseHub) {
        this.sseHub = sseHub;
    }

    public static String topicFor(String restaurantId) {
        return "restaurant:" + restaurantId;
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void on(OrderChanged change) {
        String eventName = change.type() == OrderChanged.Type.PLACED ? "order-placed" : "order-updated";
        sseHub.publish(topicFor(change.order().getRestaurantId()), eventName, OrderResponse.from(change.order()));
    }
}

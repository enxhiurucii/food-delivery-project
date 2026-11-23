package com.delivery.webshop.order.listener;

import com.delivery.webshop.checkout.service.PaymentProcessor;
import com.delivery.webshop.order.dto.OrderView;
import com.delivery.webshop.order.enums.OrderStatus;
import com.delivery.webshop.order.model.CustomerOrder;
import com.delivery.webshop.order.model.Payment;
import com.delivery.webshop.order.repository.CustomerOrderRepository;
import com.delivery.webshop.shared.messaging.event.OrderStatusChangedEvent;
import com.delivery.webshop.shared.sse.SseHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

/** Updates the customer's projection and pushes the new timeline to the open tracking page (SSE). */
@Service
public class OrderStatusChangedHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusChangedHandler.class);

    private final CustomerOrderRepository orders;
    private final SseHub sseHub;
    private final PaymentProcessor paymentProcessor;

    public OrderStatusChangedHandler(CustomerOrderRepository orders, SseHub sseHub, PaymentProcessor paymentProcessor) {
        this.orders = orders;
        this.sseHub = sseHub;
        this.paymentProcessor = paymentProcessor;
    }

    private void maybeRefund(CustomerOrder order) {
        Payment payment = order.getPayment();
        if ((order.getStatus() == OrderStatus.REJECTED || order.getStatus() == OrderStatus.CANCELLED)
                && payment.method() == Payment.Method.CARD
                && payment.timing() == Payment.Timing.ONLINE
                && payment.reference() != null
                && payment.status() == Payment.Status.PAID) {
            try {
                paymentProcessor.refund(payment.reference());
                order.markPaymentRefunded();
                log.info("Refunded card payment {} for order {}", payment.reference(), order.getId());
            } catch (Exception e) {
                log.error("Refund failed for order {} (ref {}): {}", order.getId(), payment.reference(), e.getMessage());
            }
        }
    }

    public static String topicFor(String orderId) {
        return "order:" + orderId;
    }

    public void handle(OrderStatusChangedEvent event) {
        CustomerOrder order = orders.findById(event.orderId()).orElse(null);
        if (order == null) {
            // Should not happen (we created the order). Log and drop instead of retrying forever.
            log.warn("Status change for unknown order {} ignored", event.orderId());
            return;
        }
        boolean applied = order.applyStatusChange(OrderStatus.valueOf(event.status()), event.sequence(),
                event.paymentStatus() == null ? null : Payment.Status.valueOf(event.paymentStatus()),
                event.changedAt());
        if (!applied) {
            log.info("Stale or duplicate status event {} for order {} ignored", event.sequence(), event.orderId());
            return;
        }
        maybeRefund(order);
        CustomerOrder saved = orders.save(order);
        sseHub.publish(topicFor(saved.getId()), "order-updated", OrderView.from(saved));
    }
}

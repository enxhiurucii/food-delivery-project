package com.delivery.webshop.order.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.delivery.webshop.order.enums.FulfillmentType;
import com.delivery.webshop.order.enums.OrderStatus;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * The customer's view of an order. Created at checkout; afterwards it is a PROJECTION of the back
 * office's lifecycle, updated from "order.status.changed" events. {@code lastSequence} makes the
 * projection idempotent and robust against out-of-order delivery.
 */
@Getter
@Document("orders")
@CompoundIndex(name = "customer_placed", def = "{'customerId': 1, 'placedAt': -1}")
public class CustomerOrder {

    @Id
    private String id;
    private String customerId;
    private String customerName;
    private String restaurantId;
    private String restaurantName;
    private List<OrderLine> lines = new ArrayList<>();
    private long subtotalCents;
    private long deliveryFeeCents;
    private long totalCents;
    private FulfillmentType fulfillmentType;
    private DeliveryAddress deliveryAddress;
    private String notes;
    private Payment payment;
    private OrderStatus status;
    private List<StatusChange> statusHistory = new ArrayList<>();
    private int lastSequence;
    private Instant placedAt;
    private Instant estimatedReadyAt;
    @Version
    private Long version;

    protected CustomerOrder() {
    }

    public static CustomerOrder place(String id, String customerId, String customerName, String restaurantId,
                                      String restaurantName, List<OrderLine> lines, long deliveryFeeCents,
                                      FulfillmentType fulfillmentType, DeliveryAddress deliveryAddress, String notes,
                                      Payment payment, Instant placedAt, Instant estimatedReadyAt) {
        CustomerOrder order = new CustomerOrder();
        order.id = id;
        order.customerId = customerId;
        order.customerName = customerName;
        order.restaurantId = restaurantId;
        order.restaurantName = restaurantName;
        order.lines = new ArrayList<>(lines);
        order.subtotalCents = lines.stream().mapToLong(OrderLine::totalCents).sum();
        order.fulfillmentType = fulfillmentType;
        order.deliveryFeeCents = fulfillmentType == FulfillmentType.DELIVERY ? deliveryFeeCents : 0;
        order.totalCents = order.subtotalCents + order.deliveryFeeCents;
        order.deliveryAddress = fulfillmentType == FulfillmentType.DELIVERY ? deliveryAddress : null;
        order.notes = notes;
        order.payment = payment;
        order.status = OrderStatus.PLACED;
        order.statusHistory.add(new StatusChange(OrderStatus.PLACED, placedAt));
        order.lastSequence = 1;
        order.placedAt = placedAt;
        order.estimatedReadyAt = estimatedReadyAt;
        return order;
    }

    public void markPaymentRefunded() {
        payment = payment.withStatus(Payment.Status.REFUNDED);
    }

    /**
     * Applies a status change reported by the back office.
     *
     * @return false if the event is a duplicate or older than what we already have (ignored)
     */
    public boolean applyStatusChange(OrderStatus newStatus, int sequence, Payment.Status paymentStatus, Instant at) {
        if (sequence <= lastSequence) {
            return false;
        }
        status = newStatus;
        lastSequence = sequence;
        statusHistory.add(new StatusChange(newStatus, at));
        if (paymentStatus != null) {
            payment = payment.withStatus(paymentStatus);
        }
        return true;
    }
}

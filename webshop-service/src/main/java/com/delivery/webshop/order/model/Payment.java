package com.delivery.webshop.order.model;

/**
 * Payment choice made at checkout.
 * <ul>
 *   <li>CARD + ONLINE: "pay now", charged by the (mock) payment processor at checkout, PAID immediately.</li>
 *   <li>CASH or CARD + ON_COMPLETION, pickup: "pay at the restaurant".</li>
 *   <li>CASH + ON_COMPLETION, delivery: "cash on delivery".</li>
 * </ul>
 * Deferred payments become PAID when the back office completes the order.
 */
public record Payment(Method method, Timing timing, Status status, String reference) {

    public enum Method { CASH, CARD }

    public enum Timing { ONLINE, ON_COMPLETION }

    public enum Status { PENDING, PAID, REFUNDED }

    public Payment withStatus(Status newStatus) {
        return new Payment(method, timing, newStatus, reference);
    }
}

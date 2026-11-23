package com.delivery.backoffice.order.model;

/**
 * How the customer pays. Payments are simplified on purpose (no real PSP):
 * <ul>
 *   <li>CARD + ONLINE: charged at checkout by the webshop, arrives here already PAID.</li>
 *   <li>CASH/CARD + ON_COMPLETION: paid at the restaurant (pickup) or to the courier (delivery);
 *       becomes PAID automatically when the order is completed.</li>
 * </ul>
 */
public record Payment(Method method, Timing timing, Status status) {

    public enum Method { CASH, CARD }

    public enum Timing { ONLINE, ON_COMPLETION }

    public enum Status { PENDING, PAID }

    public Payment markPaid() {
        return new Payment(method, timing, Status.PAID);
    }
}

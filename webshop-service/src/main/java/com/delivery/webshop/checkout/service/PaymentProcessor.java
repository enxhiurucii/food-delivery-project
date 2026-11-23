package com.delivery.webshop.checkout.service;

/** Port to a payment service provider. A real implementation (Stripe, Adyen...) would plug in here. */
public interface PaymentProcessor {

    /** Charges the card online. Throws {@link PaymentDeclinedException} if refused. */
    ChargeResult charge(String orderId, long amountCents, String currency);

    /** Refunds a previously charged card by PSP reference. */
    void refund(String reference);

    record ChargeResult(String reference) {
    }

    class PaymentDeclinedException extends RuntimeException {
        public PaymentDeclinedException(String message) {
            super(message);
        }
    }
}

package com.delivery.webshop.checkout.service.impl;

import java.util.UUID;

import com.delivery.webshop.checkout.service.PaymentProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

/** As agreed for the assignment: online card payments are always approved. */
@Component
public class MockPaymentProcessor implements PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentProcessor.class);

    @Override
    public ChargeResult charge(String orderId, long amountCents, String currency) {
        String reference = "mock_" + UUID.randomUUID();
        log.info("Mock-charged {} {} cents for order {} ({})", currency, amountCents, orderId, reference);
        return new ChargeResult(reference);
    }

    @Override
    public void refund(String reference) {
        log.info("Mock-refunded payment {}", reference);
    }
}

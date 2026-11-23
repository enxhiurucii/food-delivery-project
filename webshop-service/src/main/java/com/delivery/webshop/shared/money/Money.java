package com.delivery.webshop.shared.money;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Amounts are persisted as long minor units (cents) to avoid floating point errors and to keep
 * MongoDB aggregations ($sum) exact. The public API exposes BigDecimal with 2 decimals.
 */
public final class Money {

    public static final String CURRENCY = "EUR";

    private Money() {
    }

    public static BigDecimal toDecimal(long cents) {
        return BigDecimal.valueOf(cents, 2);
    }

    public static long toCents(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }
}

package com.delivery.backoffice.order.model;

/**
 * In-process application event, published inside the transaction and delivered to listeners only
 * AFTER commit (see OrderSseNotifier), so the UI is never told about a change that was rolled back.
 */
public record OrderChanged(Order order, Type type) {

    public enum Type { PLACED, STATUS_CHANGED }
}

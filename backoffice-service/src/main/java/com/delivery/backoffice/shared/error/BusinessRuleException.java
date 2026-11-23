package com.delivery.backoffice.shared.error;

import lombok.Getter;

/** A request that is well-formed but violates a business rule (HTTP 422). */
@Getter
public class BusinessRuleException extends RuntimeException {

    private final String code;

    public BusinessRuleException(String code, String message) {
        super(message);
        this.code = code;
    }
}

package com.delivery.webshop.shared.error;

/** The back office could not be reached or failed (HTTP 503 to the customer, safe to retry). */
public class BackofficeUnavailableException extends RuntimeException {

    public BackofficeUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

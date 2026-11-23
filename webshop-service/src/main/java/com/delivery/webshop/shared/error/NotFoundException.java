package com.delivery.webshop.shared.error;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String resource, String id) {
        super(resource + " '" + id + "' was not found");
    }
}

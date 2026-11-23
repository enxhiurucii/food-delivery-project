package com.delivery.webshop.shared.security;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * The logged-in customer, taken from the validated Keycloak token. {@code id} is the immutable token
 * subject and is used as the owner key of carts and orders (never the email, which can change).
 */
public record CurrentCustomer(String id, String name, String email) {

    public static CurrentCustomer from(Jwt jwt) {
        String name = jwt.getClaimAsString("name");
        if (name == null || name.isBlank()) {
            name = jwt.getClaimAsString("preferred_username");
        }
        return new CurrentCustomer(jwt.getSubject(), name, jwt.getClaimAsString("email"));
    }
}

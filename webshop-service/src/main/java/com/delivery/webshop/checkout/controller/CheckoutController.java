package com.delivery.webshop.checkout.controller;

import com.delivery.webshop.checkout.dto.CheckoutRequest;
import com.delivery.webshop.checkout.dto.CheckoutResponse;
import com.delivery.webshop.checkout.service.CheckoutService;
import com.delivery.webshop.checkout.service.CheckoutService.CheckoutResult;
import com.delivery.webshop.order.dto.OrderView;
import com.delivery.webshop.shared.security.CurrentCustomer;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final CheckoutService checkout;

    public CheckoutController(CheckoutService checkout) {
        this.checkout = checkout;
    }

    /** Turns the customer's cart into an order. Response 201 with the order (redirect to the tracking page). */
    @PostMapping("/api/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public CheckoutResponse processCheckout(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CheckoutRequest request) {
        CheckoutResult result = checkout.processCheckout(CurrentCustomer.from(jwt), request.toCommand());
        return new CheckoutResponse(OrderView.from(result.order()), result.pricesChanged());
    }
}

package com.delivery.webshop.checkout.service;

import com.delivery.webshop.checkout.dto.CheckoutCommand;
import com.delivery.webshop.order.model.CustomerOrder;
import com.delivery.webshop.shared.security.CurrentCustomer;

public interface CheckoutService {

    CheckoutResult processCheckout(CurrentCustomer customer, CheckoutCommand command);

    record CheckoutResult(CustomerOrder order, boolean pricesChanged) {
    }
}

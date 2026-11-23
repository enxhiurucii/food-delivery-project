package com.delivery.webshop.checkout.service.impl;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.delivery.webshop.cart.model.Cart;
import com.delivery.webshop.cart.repository.CartRepository;
import com.delivery.webshop.catalog.dto.PricingRequest;
import com.delivery.webshop.catalog.dto.PricingResponse;
import com.delivery.webshop.catalog.dto.RequestedLine;
import com.delivery.webshop.catalog.service.CatalogService;
import com.delivery.webshop.checkout.dto.CheckoutCommand;
import com.delivery.webshop.checkout.service.CheckoutService;
import com.delivery.webshop.checkout.service.PaymentProcessor;
import com.delivery.webshop.order.enums.FulfillmentType;
import com.delivery.webshop.order.model.CustomerOrder;
import com.delivery.webshop.order.model.OrderLine;
import com.delivery.webshop.order.model.Payment;
import com.delivery.webshop.order.model.PaymentPolicy;
import com.delivery.webshop.order.repository.CustomerOrderRepository;
import com.delivery.webshop.shared.error.BusinessRuleException;
import com.delivery.webshop.shared.messaging.RoutingKeys;
import com.delivery.webshop.shared.messaging.event.OrderPlacedEvent;
import com.delivery.webshop.shared.messaging.outbox.Outbox;
import com.delivery.webshop.shared.money.Money;
import com.delivery.webshop.shared.security.CurrentCustomer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Checkout = turn the cart into an order.
 * <ol>
 *   <li>Re-price every line against the back office (sync HTTP): the browser's prices are never trusted.</li>
 *   <li>Apply the ordering rules: restaurant open, fulfillment type offered, address, minimum order, payment option.</li>
 *   <li>Charge online card payments (mock PSP).</li>
 *   <li>In ONE MongoDB transaction: save the order, write the "order.placed" outbox event, delete the cart.</li>
 * </ol>
 * The back office then receives the order asynchronously via RabbitMQ.
 */
@Service
public class CheckoutServiceImpl implements CheckoutService {

    private final CartRepository carts;
    private final CustomerOrderRepository orders;
    private final CatalogService backoffice;
    private final PaymentProcessor paymentProcessor;
    private final Outbox outbox;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public CheckoutServiceImpl(CartRepository carts, CustomerOrderRepository orders, CatalogService backoffice,
                               PaymentProcessor paymentProcessor, Outbox outbox,
                               PlatformTransactionManager transactionManager, Clock clock) {
        this.carts = carts;
        this.orders = orders;
        this.backoffice = backoffice;
        this.paymentProcessor = paymentProcessor;
        this.outbox = outbox;
        this.transaction = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    @Override
    public CheckoutResult processCheckout(CurrentCustomer customer, CheckoutCommand command) {
        Cart cart = carts.findById(customer.id())
                .filter(c -> !c.isEmpty())
                .orElseThrow(() -> new BusinessRuleException("CART_EMPTY", "Your cart is empty"));

        PricingResponse pricing = backoffice.calculatePrice(cart.getRestaurantId(), new PricingRequest(
                cart.getItems().stream().map(i -> new RequestedLine(i.menuItemId(), i.quantity())).toList()));

        validate(command, pricing);

        String orderId = UUID.randomUUID().toString();
        long deliveryFee = command.fulfillmentType() == FulfillmentType.DELIVERY ? pricing.deliveryFeeCents() : 0;
        long total = pricing.subtotalCents() + deliveryFee;
        Payment payment = pay(orderId, total, command);
        Instant now = clock.instant();

        List<OrderLine> lines = pricing.lines().stream()
                .map(l -> new OrderLine(l.menuItemId(), l.name(), l.unitPriceCents(), l.quantity()))
                .toList();
        CustomerOrder order = CustomerOrder.place(orderId, customer.id(), customer.name(), pricing.restaurantId(),
                pricing.restaurantName(), lines, deliveryFee, command.fulfillmentType(), command.deliveryAddress(),
                command.notes(), payment, now, now.plus(Duration.ofMinutes(pricing.estimatedPrepMinutes())));

        CustomerOrder saved = transaction.execute(status -> {
            CustomerOrder persisted = orders.save(order);
            UUID eventId = UUID.randomUUID();
            outbox.enqueue(RoutingKeys.ORDER_PLACED, eventId, toEvent(eventId, persisted));
            carts.deleteById(customer.id());
            return persisted;
        });

        boolean pricesChanged = cart.subtotalCents() != pricing.subtotalCents();
        return new CheckoutResult(saved, pricesChanged);
    }

    private static void validate(CheckoutCommand command, PricingResponse pricing) {
        if (!pricing.acceptingOrders()) {
            throw new BusinessRuleException("RESTAURANT_CLOSED", pricing.restaurantName() + " is not accepting orders right now");
        }
        if (command.fulfillmentType() == FulfillmentType.PICKUP && !pricing.pickupEnabled()) {
            throw new BusinessRuleException("PICKUP_NOT_AVAILABLE", "This restaurant does not offer pickup");
        }
        if (command.fulfillmentType() == FulfillmentType.DELIVERY) {
            if (!pricing.deliveryEnabled()) {
                throw new BusinessRuleException("DELIVERY_NOT_AVAILABLE", "This restaurant does not deliver");
            }
            if (command.deliveryAddress() == null) {
                throw new BusinessRuleException("ADDRESS_REQUIRED", "A delivery address is required");
            }
        }
        if (pricing.subtotalCents() < pricing.minimumOrderCents()) {
            throw new BusinessRuleException("MINIMUM_ORDER_NOT_REACHED",
                    "Minimum order is " + Money.toDecimal(pricing.minimumOrderCents()) + " " + Money.CURRENCY);
        }
        PaymentPolicy.validate(command.fulfillmentType(), command.paymentMethod(), command.paymentTiming());
    }

    private Payment pay(String orderId, long totalCents, CheckoutCommand command) {
        if (command.paymentTiming() == Payment.Timing.ONLINE) {
            // With a real PSP: if the DB transaction fails after a successful charge, refund it (compensation).
            try {
                String reference = paymentProcessor.charge(orderId, totalCents, Money.CURRENCY).reference();
                return new Payment(command.paymentMethod(), command.paymentTiming(), Payment.Status.PAID, reference);
            } catch (PaymentProcessor.PaymentDeclinedException ex) {
                throw new BusinessRuleException("PAYMENT_DECLINED", ex.getMessage());
            }
        }
        return new Payment(command.paymentMethod(), command.paymentTiming(), Payment.Status.PENDING, null);
    }

    private static OrderPlacedEvent toEvent(UUID eventId, CustomerOrder order) {
        OrderPlacedEvent.Address address = order.getDeliveryAddress() == null ? null : new OrderPlacedEvent.Address(
                order.getDeliveryAddress().street(), order.getDeliveryAddress().city(),
                order.getDeliveryAddress().postalCode(), order.getDeliveryAddress().phone(),
                order.getDeliveryAddress().instructions());
        return new OrderPlacedEvent(eventId, order.getPlacedAt(), order.getId(), order.getRestaurantId(),
                order.getCustomerId(), order.getCustomerName(),
                order.getLines().stream()
                        .map(l -> new OrderPlacedEvent.Line(l.menuItemId(), l.name(), l.unitPriceCents(), l.quantity()))
                        .toList(),
                order.getSubtotalCents(), order.getDeliveryFeeCents(), order.getTotalCents(),
                order.getFulfillmentType().name(), address, order.getNotes(),
                order.getPayment().method().name(), order.getPayment().timing().name(),
                order.getPayment().status().name());
    }
}

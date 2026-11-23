package com.delivery.webshop.checkout.application

import com.delivery.webshop.cart.model.Cart
import com.delivery.webshop.cart.repository.CartRepository
import com.delivery.webshop.catalog.dto.PricedLine
import com.delivery.webshop.catalog.dto.PricingResponse
import com.delivery.webshop.catalog.service.CatalogService
import com.delivery.webshop.checkout.dto.CheckoutCommand
import com.delivery.webshop.checkout.service.CheckoutService
import com.delivery.webshop.checkout.service.PaymentProcessor
import com.delivery.webshop.checkout.service.impl.CheckoutServiceImpl
import com.delivery.webshop.order.enums.FulfillmentType
import com.delivery.webshop.order.enums.OrderStatus
import com.delivery.webshop.order.model.CustomerOrder
import com.delivery.webshop.order.model.DeliveryAddress
import com.delivery.webshop.order.model.Payment
import com.delivery.webshop.order.repository.CustomerOrderRepository
import com.delivery.webshop.shared.error.BusinessRuleException
import com.delivery.webshop.shared.messaging.RoutingKeys
import com.delivery.webshop.shared.messaging.event.OrderPlacedEvent
import com.delivery.webshop.shared.messaging.outbox.Outbox
import com.delivery.webshop.shared.security.CurrentCustomer
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import spock.lang.Specification
import spock.lang.Subject

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class CheckoutServiceSpec extends Specification {

    def now = Instant.parse('2026-11-23T12:00:00Z')
    def customer = new CurrentCustomer('customer-1', 'Alice', 'alice@example.com')
    def address = new DeliveryAddress('Rruga 1', 'Tirana', '1001', '+355690000000', null)

    CartRepository carts = Mock()
    CustomerOrderRepository orders = Mock()
    CatalogService backoffice = Mock()
    PaymentProcessor paymentProcessor = Mock()
    Outbox outbox = Mock()
    PlatformTransactionManager transactionManager = Stub() {
        getTransaction(_) >> Stub(TransactionStatus)
    }

    @Subject
    CheckoutService service = new CheckoutServiceImpl(carts, orders, backoffice, paymentProcessor, outbox,
            transactionManager, Clock.fixed(now, ZoneOffset.UTC))

    def setup() {
        def cart = Cart.emptyFor('customer-1', now)
        cart.addItem('napoli', 'burger', 'Burger menu', 1000, 2, now)
        carts.findById('customer-1') >> Optional.of(cart)
        orders.save(_ as CustomerOrder) >> { CustomerOrder order -> order }
    }

    def "a delivery checkout re-prices the cart, saves the order, enqueues order.placed and empties the cart"() {
        given:
        backoffice.calculatePrice('napoli', _) >> pricing(1100)  // price went up since the item was added

        when:
        def result = service.processCheckout(customer, command(FulfillmentType.DELIVERY, Payment.Method.CASH, Payment.Timing.ON_COMPLETION))

        then:
        result.pricesChanged()
        with(result.order()) {
            status == OrderStatus.PLACED
            subtotalCents == 2200
            deliveryFeeCents == 250
            totalCents == 2450
            payment.status() == Payment.Status.PENDING
            customerId == 'customer-1'
            estimatedReadyAt == now.plusSeconds(20 * 60)
        }
        1 * outbox.enqueue(RoutingKeys.ORDER_PLACED, _ as UUID, { OrderPlacedEvent e ->
            e.restaurantId() == 'napoli' && e.totalCents() == 2450 && e.fulfillmentType() == 'DELIVERY' &&
                    e.deliveryAddress().city() == 'Tirana' && e.lines()[0].unitPriceCents() == 1100
        })
        1 * carts.deleteById('customer-1')
        0 * paymentProcessor._
    }

    def "paying online charges the card first and the order is created as PAID"() {
        given:
        backoffice.calculatePrice('napoli', _) >> pricing(1000)

        when:
        def result = service.processCheckout(customer, command(FulfillmentType.PICKUP, Payment.Method.CARD, Payment.Timing.ONLINE))

        then:
        1 * paymentProcessor.charge(_ as String, 2000, 'EUR') >> new PaymentProcessor.ChargeResult('mock_123')
        result.order().payment.status() == Payment.Status.PAID
        result.order().payment.reference() == 'mock_123'
        result.order().deliveryFeeCents == 0
        !result.pricesChanged()
    }

    def "checkout is refused with #code"() {
        given:
        backoffice.calculatePrice('napoli', _) >> pricing(1000, accepting, pickup, delivery, minimum)

        when:
        service.processCheckout(customer, new CheckoutCommand(type, withAddress ? address : null, method, timing, null))

        then:
        def ex = thrown(BusinessRuleException)
        ex.code == code
        0 * orders.save(_)
        0 * outbox._
        0 * paymentProcessor._

        where:
        accepting | pickup | delivery | minimum | type                     | withAddress | method              | timing                       || code
        false     | true   | true     | 0       | FulfillmentType.PICKUP   | false       | Payment.Method.CASH | Payment.Timing.ON_COMPLETION || 'RESTAURANT_CLOSED'
        true      | false  | true     | 0       | FulfillmentType.PICKUP   | false       | Payment.Method.CASH | Payment.Timing.ON_COMPLETION || 'PICKUP_NOT_AVAILABLE'
        true      | true   | false    | 0       | FulfillmentType.DELIVERY | true        | Payment.Method.CASH | Payment.Timing.ON_COMPLETION || 'DELIVERY_NOT_AVAILABLE'
        true      | true   | true     | 0       | FulfillmentType.DELIVERY | false       | Payment.Method.CASH | Payment.Timing.ON_COMPLETION || 'ADDRESS_REQUIRED'
        true      | true   | true     | 5000    | FulfillmentType.PICKUP   | false       | Payment.Method.CASH | Payment.Timing.ON_COMPLETION || 'MINIMUM_ORDER_NOT_REACHED'
        true      | true   | true     | 0       | FulfillmentType.DELIVERY | true        | Payment.Method.CARD | Payment.Timing.ON_COMPLETION || 'PAYMENT_OPTION_NOT_ALLOWED'
    }

    def "an empty cart cannot be checked out"() {
        when:
        service.processCheckout(new CurrentCustomer('someone-else', 'Bob', null),
                command(FulfillmentType.PICKUP, Payment.Method.CASH, Payment.Timing.ON_COMPLETION))

        then:
        carts.findById('someone-else') >> Optional.empty()
        def ex = thrown(BusinessRuleException)
        ex.code == 'CART_EMPTY'
    }

    private CheckoutCommand command(FulfillmentType type, Payment.Method method, Payment.Timing timing) {
        new CheckoutCommand(type, type == FulfillmentType.DELIVERY ? address : null, method, timing, 'No onions')
    }

    private static PricingResponse pricing(long burgerPrice, boolean accepting = true, boolean pickup = true,
                                           boolean delivery = true, long minimum = 1000) {
        new PricingResponse('napoli', 'Pizzeria Napoli', accepting, pickup, delivery, 250, minimum, 20,
                [new PricedLine('burger', 'Burger menu', burgerPrice, 2)])
    }
}

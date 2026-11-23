package com.delivery.webshop.order.domain

import com.delivery.webshop.order.enums.FulfillmentType
import com.delivery.webshop.order.model.Payment
import com.delivery.webshop.order.model.PaymentPolicy
import com.delivery.webshop.shared.error.BusinessRuleException
import spock.lang.Specification

import static com.delivery.webshop.order.enums.FulfillmentType.DELIVERY
import static com.delivery.webshop.order.enums.FulfillmentType.PICKUP
import static com.delivery.webshop.order.model.Payment.Method.CARD
import static com.delivery.webshop.order.model.Payment.Method.CASH
import static com.delivery.webshop.order.model.Payment.Timing.ON_COMPLETION
import static com.delivery.webshop.order.model.Payment.Timing.ONLINE

class PaymentPolicySpec extends Specification {

    def "#fulfillment with #method / #timing is allowed (#option)"() {
        when:
        PaymentPolicy.validate(fulfillment, method, timing)

        then:
        noExceptionThrown()

        where:
        fulfillment | method | timing        || option
        PICKUP      | CARD   | ONLINE        || 'pay now'
        DELIVERY    | CARD   | ONLINE        || 'pay now'
        PICKUP      | CASH   | ON_COMPLETION || 'pay at the restaurant'
        PICKUP      | CARD   | ON_COMPLETION || 'pay at the restaurant by card'
        DELIVERY    | CASH   | ON_COMPLETION || 'cash on delivery'
    }

    def "#fulfillment with #method / #timing is rejected with #code"() {
        when:
        PaymentPolicy.validate(fulfillment, method, timing)

        then:
        def ex = thrown(BusinessRuleException)
        ex.code == code

        where:
        fulfillment | method | timing        || code
        PICKUP      | CASH   | ONLINE        || 'PAYMENT_OPTION_NOT_ALLOWED'  // cash can't be paid online
        DELIVERY    | CASH   | ONLINE        || 'PAYMENT_OPTION_NOT_ALLOWED'
        DELIVERY    | CARD   | ON_COMPLETION || 'PAYMENT_OPTION_NOT_ALLOWED'  // couriers take cash only
        PICKUP      | null   | ONLINE        || 'PAYMENT_REQUIRED'
        DELIVERY    | CASH   | null          || 'PAYMENT_REQUIRED'
    }
}

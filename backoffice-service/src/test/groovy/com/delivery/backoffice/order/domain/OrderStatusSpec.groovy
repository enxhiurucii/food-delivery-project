package com.delivery.backoffice.order.domain

import spock.lang.Specification

import static com.delivery.backoffice.order.enums.FulfillmentType.DELIVERY
import static com.delivery.backoffice.order.enums.FulfillmentType.PICKUP
import static com.delivery.backoffice.order.enums.OrderStatus.*

class OrderStatusSpec extends Specification {

    def "#from -> #to for a #type order is allowed: #allowed"() {
        expect:
        from.canTransitionTo(to, type) == allowed

        where:
        from             | to               | type     || allowed
        PLACED           | ACCEPTED         | PICKUP   || true
        PLACED           | REJECTED         | DELIVERY || true
        PLACED           | CANCELLED        | PICKUP   || true
        PLACED           | PREPARING        | PICKUP   || false
        ACCEPTED         | PREPARING        | DELIVERY || true
        ACCEPTED         | CANCELLED        | DELIVERY || true
        ACCEPTED         | REJECTED         | PICKUP   || false
        PREPARING        | READY_FOR_PICKUP | PICKUP   || true
        PREPARING        | OUT_FOR_DELIVERY | PICKUP   || false
        PREPARING        | OUT_FOR_DELIVERY | DELIVERY || false
        PREPARING        | READY_FOR_PICKUP | DELIVERY || true
        PREPARING        | CANCELLED        | PICKUP   || false
        READY_FOR_PICKUP | PICKED_UP        | PICKUP   || true
        READY_FOR_PICKUP | OUT_FOR_DELIVERY | DELIVERY || true
        OUT_FOR_DELIVERY | DELIVERED        | DELIVERY || true
        OUT_FOR_DELIVERY | PICKED_UP        | DELIVERY || false
        DELIVERED        | PLACED           | DELIVERY || false
        CANCELLED        | ACCEPTED         | PICKUP   || false
    }

    def "terminal and completed statuses"() {
        expect:
        status.terminal == terminal
        status.completed == completed

        where:
        status           || terminal | completed
        PLACED           || false    | false
        PREPARING        || false    | false
        PICKED_UP        || true     | true
        DELIVERED        || true     | true
        REJECTED         || true     | false
        CANCELLED        || true     | false
    }
}

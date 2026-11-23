package com.delivery.webshop.cart.domain

import com.delivery.webshop.cart.model.Cart
import com.delivery.webshop.shared.error.BusinessRuleException
import spock.lang.Specification

import java.time.Instant

class CartSpec extends Specification {

    def now = Instant.parse('2026-11-23T10:00:00Z')
    def cart = Cart.emptyFor('customer-1', now)

    def "adding the same item twice increases its quantity"() {
        when:
        cart.addItem('napoli', 'burger', 'Burger menu', 1000, 1, now)
        cart.addItem('napoli', 'burger', 'Burger menu', 1000, 2, now)

        then:
        cart.items.size() == 1
        cart.items[0].quantity() == 3
        cart.subtotalCents() == 3000
        cart.restaurantId == 'napoli'
    }

    def "a cart only holds items of one restaurant"() {
        given:
        cart.addItem('napoli', 'burger', 'Burger menu', 1000, 1, now)

        when:
        cart.addItem('sushi-zen', 'maki', 'Salmon maki', 900, 1, now)

        then:
        def ex = thrown(BusinessRuleException)
        ex.code == 'CART_RESTAURANT_MISMATCH'
        cart.items*.menuItemId() == ['burger']
    }

    def "setting quantity to 0 removes the item and frees the cart for another restaurant"() {
        given:
        cart.addItem('napoli', 'burger', 'Burger menu', 1000, 2, now)

        when:
        cart.changeQuantity('burger', 0, now)
        cart.addItem('sushi-zen', 'maki', 'Salmon maki', 900, 1, now)

        then:
        cart.restaurantId == 'sushi-zen'
        cart.items*.menuItemId() == ['maki']
    }

    def "quantities are capped to protect against abuse"() {
        when:
        cart.addItem('napoli', 'burger', 'Burger menu', 1000, 40, now)
        cart.addItem('napoli', 'burger', 'Burger menu', 1000, 40, now)

        then:
        cart.items[0].quantity() == Cart.MAX_QUANTITY_PER_ITEM
    }

    def "changing an item that is not in the cart is rejected"() {
        when:
        cart.changeQuantity('ghost', 1, now)

        then:
        def ex = thrown(BusinessRuleException)
        ex.code == 'ITEM_NOT_IN_CART'
    }
}

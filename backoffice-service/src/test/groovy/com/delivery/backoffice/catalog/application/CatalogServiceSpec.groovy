package com.delivery.backoffice.catalog.application

import com.delivery.backoffice.catalog.dto.PricingRequest
import com.delivery.backoffice.catalog.dto.RequestedLine
import com.delivery.backoffice.catalog.service.CatalogService
import com.delivery.backoffice.catalog.service.impl.CatalogServiceImpl
import com.delivery.backoffice.menu.model.Menu
import com.delivery.backoffice.menu.repository.MenuRepository
import com.delivery.backoffice.restaurant.model.Restaurant
import com.delivery.backoffice.restaurant.model.RestaurantDetails
import com.delivery.backoffice.restaurant.service.RestaurantService
import com.delivery.backoffice.shared.error.BusinessRuleException
import spock.lang.Specification
import spock.lang.Subject

import java.time.Instant

class CatalogServiceSpec extends Specification {

    def now = Instant.parse('2026-11-23T10:00:00Z')
    RestaurantService restaurants = Stub()
    MenuRepository menus = Stub()

    @Subject
    CatalogService catalog = new CatalogServiceImpl(restaurants, menus)

    Menu lunch = Menu.create('napoli', 'Lunch', null, 1, now)
    def burger = lunch.addItem('Burger menu', null, 1000, null, true, now)
    def soldOut = lunch.addItem('Tiramisu', null, 500, null, false, now)

    def setup() {
        restaurants.getRestaurantById('napoli') >> Restaurant.create('napoli', new RestaurantDetails('Napoli', null, 'Street 1', null,
                null, 'Europe/Tirane', true, true, true, 250, 1000, 20), now)
        menus.findByRestaurantIdOrderByPositionAsc('napoli') >> [lunch]
    }

    def "pricing returns the authoritative price and the restaurant's ordering rules"() {
        when:
        def response = catalog.calculatePrice('napoli', new PricingRequest([new RequestedLine(burger.id(), 3)]))

        then:
        response.lines().size() == 1
        response.lines()[0].unitPriceCents() == 1000
        response.lines()[0].quantity() == 3
        response.deliveryFeeCents() == 250
        response.minimumOrderCents() == 1000
        response.acceptingOrders()
    }

    def "pricing fails with #code"() {
        when:
        catalog.calculatePrice('napoli', new PricingRequest([new RequestedLine(itemId, 1)]))

        then:
        def ex = thrown(BusinessRuleException)
        ex.code == code

        where:
        itemId          || code
        'does-not-exist' || 'ITEM_NOT_FOUND'
        null            || 'ITEM_NOT_FOUND'
    }

    def "unavailable items cannot be ordered"() {
        when:
        catalog.calculatePrice('napoli', new PricingRequest([new RequestedLine(soldOut.id(), 1)]))

        then:
        def ex = thrown(BusinessRuleException)
        ex.code == 'ITEM_UNAVAILABLE'
    }
}

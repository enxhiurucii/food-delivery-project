package com.delivery.webshop

import com.delivery.webshop.cart.controller.CartController
import com.delivery.webshop.cart.model.Cart
import com.delivery.webshop.cart.service.CartService
import com.delivery.webshop.catalog.controller.CatalogController
import com.delivery.webshop.catalog.dto.CatalogRestaurant
import com.delivery.webshop.catalog.service.CatalogService
import com.delivery.webshop.order.controller.OrderController
import com.delivery.webshop.order.service.OrderTrackingService
import com.delivery.webshop.shared.error.NotFoundException
import com.delivery.webshop.shared.security.SecurityConfig
import com.delivery.webshop.shared.sse.SseHub
import com.delivery.webshop.shared.sse.SseTicketService
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import java.time.Instant

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** Web slice with the real SecurityConfig: public browsing, customer-only cart/orders, ownership via token subject. */
@WebMvcTest([CatalogController, CartController, OrderController])
@Import([SecurityConfig, TestCacheConfig])
class WebSecuritySpec extends Specification {

    @Autowired
    MockMvc mvc

    @SpringBean
    CatalogService backoffice = Mock()
    @SpringBean
    CartService cartService = Mock()
    @SpringBean
    OrderTrackingService tracking = Mock()
    @SpringBean
    SseHub sseHub = Mock()
    @SpringBean
    SseTicketService tickets = Mock()
    @SpringBean
    JwtDecoder jwtDecoder = Mock()

    def "anyone can browse restaurants without logging in"() {
        given:
        backoffice.getAllRestaurants() >> [new CatalogRestaurant('napoli', 'Pizzeria Napoli', null, 'Street 1', null,
                true, true, true, 250, 1000, 20)]

        expect:
        mvc.perform(get('/api/restaurants'))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$[0].name').value('Pizzeria Napoli'))
                .andExpect(jsonPath('$[0].deliveryFee').value(2.5d))
    }

    def "the cart requires login"() {
        expect:
        mvc.perform(get('/api/cart')).andExpect(status().isUnauthorized())
    }

    def "a token without the CUSTOMER role (e.g. restaurant staff) cannot use the cart"() {
        expect:
        mvc.perform(get('/api/cart').with(jwt().authorities(new SimpleGrantedAuthority('ROLE_STAFF'))))
                .andExpect(status().isForbidden())
    }

    def "the cart is loaded for the customer identified by the token subject"() {
        when:
        def result = mvc.perform(get('/api/cart').with(customer('customer-42')))

        then:
        1 * cartService.getCartByCustomer('customer-42') >> Cart.emptyFor('customer-42', Instant.now())
        result.andReturn().response.status == 200
    }

    def "another customer's order is answered with 404, not 403, so order ids cannot be probed"() {
        given:
        tracking.getOrderByCustomer('customer-42', 'order-of-someone-else') >> { throw new NotFoundException('Order', 'order-of-someone-else') }

        expect:
        mvc.perform(get('/api/orders/order-of-someone-else').with(customer('customer-42')))
                .andExpect(status().isNotFound())
    }

    def "the order stream needs a valid one-time ticket"() {
        given:
        tickets.redeem('expired', 'order:o-1') >> Optional.empty()

        expect:
        mvc.perform(get('/api/orders/o-1/stream').param('ticket', 'expired'))
                .andExpect(status().isUnauthorized())
    }

    private static customer(String subject) {
        jwt().jwt { it.subject(subject).claim('name', 'Alice') }.authorities(new SimpleGrantedAuthority('ROLE_CUSTOMER'))
    }
}

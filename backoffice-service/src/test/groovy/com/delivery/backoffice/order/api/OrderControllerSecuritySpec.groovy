package com.delivery.backoffice.order.api

import com.delivery.backoffice.OrderFixtures
import com.delivery.backoffice.order.controller.OrderController
import com.delivery.backoffice.order.enums.FulfillmentType
import com.delivery.backoffice.order.enums.OrderStatus
import com.delivery.backoffice.order.exception.InvalidStatusTransitionException
import com.delivery.backoffice.order.service.OrderService
import com.delivery.backoffice.shared.security.SecurityConfig
import com.delivery.backoffice.shared.security.TenantGuard
import com.delivery.backoffice.shared.sse.SseHub
import com.delivery.backoffice.shared.sse.SseTicketService
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Web slice: real SecurityConfig + method security + controller + exception handler, mocked services.
 * Tokens are simulated with spring-security-test's jwt() so no Keycloak is needed.
 */
@WebMvcTest(OrderController)
@Import(SecurityConfig)
class OrderControllerSecuritySpec extends Specification {

    @Autowired
    MockMvc mvc

    @SpringBean
    OrderService orderService = Mock()
    @SpringBean
    SseHub sseHub = Mock()
    @SpringBean
    SseTicketService tickets = Mock()
    @SpringBean
    TenantGuard tenantGuard = Mock()
    @SpringBean
    JwtDecoder jwtDecoder = Mock()

    def "anonymous requests get 401"() {
        expect:
        mvc.perform(get('/api/restaurants/napoli/orders')).andExpect(status().isUnauthorized())
    }

    def "a token without a staff role (e.g. a customer) gets 403"() {
        expect:
        mvc.perform(get('/api/restaurants/napoli/orders').with(withRole('CUSTOMER')))
                .andExpect(status().isForbidden())
    }

    def "staff of another restaurant gets 403"() {
        given:
        tenantGuard.canAccess('napoli', _) >> false

        expect:
        mvc.perform(get('/api/restaurants/napoli/orders').with(withRole('STAFF')))
                .andExpect(status().isForbidden())
    }

    def "staff of the restaurant sees the order board with the valid next actions"() {
        given:
        tenantGuard.canAccess('napoli', _) >> true
        orderService.getOrdersByRestaurant('napoli', _) >> [OrderFixtures.pickupOrder('o-1')]

        expect:
        mvc.perform(get('/api/restaurants/napoli/orders').with(withRole('STAFF')))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$[0].id').value('o-1'))
                .andExpect(jsonPath('$[0].total').value(20.0d))
                .andExpect(jsonPath('$[0].allowedNextStatuses[0]').value('ACCEPTED'))
    }

    def "an invalid status transition is answered with 409 and a problem detail"() {
        given:
        tenantGuard.canAccess('napoli', _) >> true
        orderService.updateOrderStatus('napoli', 'o-1', OrderStatus.DELIVERED) >> {
            throw new InvalidStatusTransitionException('o-1', OrderStatus.PLACED, OrderStatus.DELIVERED, FulfillmentType.PICKUP)
        }

        expect:
        mvc.perform(post('/api/restaurants/napoli/orders/o-1/status').with(withRole('STAFF'))
                .contentType(MediaType.APPLICATION_JSON).content('{"status":"DELIVERED"}'))
                .andExpect(status().isConflict())
                .andExpect(jsonPath('$.code').value('INVALID_STATUS_TRANSITION'))
    }

    def "a missing status in the request body is a 400"() {
        given:
        tenantGuard.canAccess('napoli', _) >> true

        expect:
        mvc.perform(post('/api/restaurants/napoli/orders/o-1/status').with(withRole('STAFF'))
                .contentType(MediaType.APPLICATION_JSON).content('{}'))
                .andExpect(status().isBadRequest())
    }

    def "the internal catalog API rejects staff tokens (service account role required)"() {
        expect:
        mvc.perform(get('/internal/restaurants').with(withRole('MANAGER')))
                .andExpect(status().isForbidden())
    }

    def "the SSE stream rejects an unknown or reused ticket"() {
        given:
        tickets.redeem('stolen', 'restaurant:napoli') >> Optional.empty()

        expect:
        mvc.perform(get('/api/restaurants/napoli/orders/stream').param('ticket', 'stolen'))
                .andExpect(status().isUnauthorized())
    }

    private static withRole(String role) {
        jwt().jwt { it.subject('user-1') }.authorities(new SimpleGrantedAuthority("ROLE_$role"))
    }
}

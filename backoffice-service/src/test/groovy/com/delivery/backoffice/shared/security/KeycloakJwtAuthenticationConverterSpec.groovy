package com.delivery.backoffice.shared.security

import org.springframework.security.oauth2.jwt.Jwt
import spock.lang.Specification

class KeycloakJwtAuthenticationConverterSpec extends Specification {

    def converter = new KeycloakJwtAuthenticationConverter()

    def "realm roles from realm_access.roles become ROLE_ authorities and scopes are kept"() {
        given:
        def jwt = Jwt.withTokenValue('token').header('alg', 'none')
                .subject('22222222-2222-2222-2222-222222222222')
                .claim('preferred_username', 'napoli.manager')
                .claim('scope', 'openid profile')
                .claim('realm_access', [roles: ['MANAGER', 'STAFF']])
                .build()

        when:
        def authentication = converter.convert(jwt)

        then:
        authentication.name == 'napoli.manager'
        authentication.authorities*.authority.toSet() ==
                ['ROLE_MANAGER', 'ROLE_STAFF', 'SCOPE_openid', 'SCOPE_profile'] as Set
    }

    def "a token without realm roles gets no ROLE_ authorities"() {
        given:
        def jwt = Jwt.withTokenValue('token').header('alg', 'none').subject('s').claim('scope', 'openid').build()

        expect:
        converter.convert(jwt).authorities*.authority == ['SCOPE_openid']
    }
}

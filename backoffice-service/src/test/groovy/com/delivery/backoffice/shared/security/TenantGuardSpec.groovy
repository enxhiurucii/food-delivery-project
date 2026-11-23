package com.delivery.backoffice.shared.security

import com.delivery.backoffice.staff.model.StaffMember
import com.delivery.backoffice.staff.repository.StaffMemberRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import spock.lang.Specification
import spock.lang.Subject

import java.time.Instant

class TenantGuardSpec extends Specification {

    StaffMemberRepository staffMembers = Stub() {
        findById('napoli-manager') >> Optional.of(new StaffMember('napoli-manager', 'napoli', 'Manager', Instant.now()))
        findById(_) >> Optional.empty()
    }

    @Subject
    TenantGuard guard = new TenantGuard(staffMembers)

    def "#description"() {
        expect:
        guard.canAccess(restaurantId, token(subject, role)) == allowed

        where:
        description                                      | subject          | role      | restaurantId || allowed
        'staff can access their own restaurant'          | 'napoli-manager' | 'MANAGER' | 'napoli'     || true
        'staff cannot access another restaurant'         | 'napoli-manager' | 'MANAGER' | 'sushi-zen'  || false
        'a user without a staff profile is denied'       | 'stranger'       | 'STAFF'   | 'napoli'     || false
        'platform admins can access every restaurant'    | 'admin'          | 'ADMIN'   | 'sushi-zen'  || true
    }

    def "missing authentication is denied"() {
        expect:
        !guard.canAccess('napoli', null)
    }

    private static JwtAuthenticationToken token(String subject, String role) {
        def jwt = Jwt.withTokenValue('token').header('alg', 'none').subject(subject).build()
        new JwtAuthenticationToken(jwt, [new SimpleGrantedAuthority("ROLE_$role")])
    }
}

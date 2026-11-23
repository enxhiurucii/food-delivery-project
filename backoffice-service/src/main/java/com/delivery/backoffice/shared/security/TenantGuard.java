package com.delivery.backoffice.shared.security;

import java.util.Optional;

import com.delivery.backoffice.staff.model.StaffMember;
import com.delivery.backoffice.staff.repository.StaffMemberRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Tenant isolation: Keycloak only says WHO the user is and WHICH role they have. Which restaurant a
 * staff member works at is back-office domain data (collection "staff_members", keyed by the token's
 * immutable "sub"). Used from controllers as {@code @PreAuthorize("@tenantGuard.canAccess(#restaurantId, authentication)")}.
 */
@Component("tenantGuard")
public class TenantGuard {

    private final StaffMemberRepository staffMembers;

    public TenantGuard(StaffMemberRepository staffMembers) {
        this.staffMembers = staffMembers;
    }

    public boolean canAccess(String restaurantId, Authentication authentication) {
        if (authentication == null || restaurantId == null) {
            return false;
        }
        if (hasRole(authentication, Roles.ADMIN)) {
            return true;
        }
        return staffMemberOf(authentication)
                .map(member -> member.getRestaurantId().equals(restaurantId))
                .orElse(false);
    }

    public Optional<StaffMember> staffMemberOf(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return staffMembers.findById(jwt.getSubject());
        }
        return Optional.empty();
    }

    private static boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> ("ROLE_" + role).equals(authority.getAuthority()));
    }
}
